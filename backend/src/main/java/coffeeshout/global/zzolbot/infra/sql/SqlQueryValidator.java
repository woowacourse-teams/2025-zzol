package coffeeshout.global.zzolbot.infra.sql;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.config.ZzolBotProperties.TableSchema;
import coffeeshout.global.zzolbot.domain.ZzolBotErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SqlQueryValidator {

    private final ZzolBotProperties properties;

    /**
     * SQL을 검증하고 LIMIT이 없거나 초과한 경우 maxRows로 보정한 SQL을 반환한다.
     *
     * @return 실행 가능한 SQL (LIMIT 자동 보정 포함)
     * @throws BusinessException 검증 실패 시
     */
    public String validate(String sql) {
        final Statements statements = parse(sql);
        validateSingleSelect(statements);

        final PlainSelect plainSelect = extractPlainSelect(statements);
        validateNoWildcard(plainSelect);
        validateAllowedTables(plainSelect);
        validateBlockedColumns(plainSelect);

        return applyLimitIfNeeded(sql, plainSelect);
    }

    private Statements parse(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new BusinessException(ZzolBotErrorCode.INVALID_SQL, "SQL이 비어있습니다.");
        }
        try {
            final Statements statements = CCJSqlParserUtil.parseStatements(sql);
            if (statements == null) {
                throw new BusinessException(ZzolBotErrorCode.INVALID_SQL, "SQL 파싱 결과가 없습니다.");
            }
            return statements;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ZzolBotErrorCode.INVALID_SQL,
                    "SQL 파싱에 실패했습니다: " + e.getMessage());
        }
    }

    private void validateSingleSelect(Statements statements) {
        final List<Statement> stmtList = statements.getStatements();
        if (stmtList.size() != 1) {
            throw new BusinessException(ZzolBotErrorCode.INVALID_SQL,
                    "단일 SELECT 문 하나만 허용됩니다. 현재 " + stmtList.size() + "개의 구문이 감지됐습니다.");
        }
        if (!(stmtList.get(0) instanceof Select)) {
            throw new BusinessException(ZzolBotErrorCode.INVALID_SQL,
                    "SELECT 문만 허용됩니다. DDL·DML은 사용할 수 없습니다.");
        }
    }

    // JSqlParser 5.x: PlainSelect extends Select extends Statement (SELECT * FROM ... 형태)
    private PlainSelect extractPlainSelect(Statements statements) {
        final Select select = (Select) statements.getStatements().get(0);
        if (!(select instanceof PlainSelect plainSelect)) {
            throw new BusinessException(ZzolBotErrorCode.INVALID_SQL,
                    "단순 SELECT 형태만 지원합니다. UNION·INTERSECT 등은 허용되지 않습니다.");
        }
        if (plainSelect.getIntoTables() != null && !plainSelect.getIntoTables().isEmpty()) {
            throw new BusinessException(ZzolBotErrorCode.INVALID_SQL,
                    "SELECT INTO는 허용되지 않습니다.");
        }
        // JSqlParser 5.x: FOR UPDATE 여부는 Select 부모 클래스의 forUpdateTable 필드
        if (plainSelect.getForUpdateTable() != null || plainSelect.getForMode() != null) {
            throw new BusinessException(ZzolBotErrorCode.INVALID_SQL,
                    "FOR UPDATE는 허용되지 않습니다.");
        }
        return plainSelect;
    }

    // JSqlParser 5.x: AllColumns/AllTableColumns 는 Expression 구현체 → item.getExpression()으로 확인
    private void validateNoWildcard(PlainSelect select) {
        final boolean hasWildcard = select.getSelectItems().stream()
                .anyMatch(item -> item.getExpression() instanceof AllColumns
                        || item.getExpression() instanceof AllTableColumns);
        if (hasWildcard) {
            throw new BusinessException(ZzolBotErrorCode.SQL_WILDCARD_NOT_ALLOWED,
                    "와일드카드(*)는 허용되지 않습니다. 조회할 컬럼을 직접 명시해 주세요.");
        }
    }

    private void validateAllowedTables(PlainSelect select) {
        final Set<String> allowedTableNames = properties.sql().allowedTables().stream()
                .map(TableSchema::name)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // PlainSelect가 Statement와 Expression 모두 구현하므로 Statement로 명시적 캐스팅
        final List<String> referencedTables = new TablesNamesFinder().getTableList((Statement) select);

        referencedTables.stream()
                .map(String::toLowerCase)
                .filter(table -> !allowedTableNames.contains(table))
                .findFirst()
                .ifPresent(blocked -> {
                    throw new BusinessException(ZzolBotErrorCode.SQL_TABLE_NOT_ALLOWED,
                            "허용되지 않은 테이블입니다: " + blocked);
                });
    }

    private void validateBlockedColumns(PlainSelect select) {
        final Map<String, Set<String>> blockedByTable = properties.sql().allowedTables().stream()
                .filter(schema -> schema.blockedColumns() != null && !schema.blockedColumns().isEmpty())
                .collect(Collectors.toMap(
                        schema -> schema.name().toLowerCase(),
                        schema -> schema.blockedColumns().stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet())
                ));

        if (blockedByTable.isEmpty()) {
            return;
        }

        for (final var item : select.getSelectItems()) {
            if (!(item.getExpression() instanceof Column column)) {
                continue;
            }
            final String colName = column.getColumnName().toLowerCase();
            final String tableName = column.getTable() != null
                    ? column.getTable().getName().toLowerCase()
                    : null;

            if (tableName != null) {
                final Set<String> blocked = blockedByTable.get(tableName);
                if (blocked != null && blocked.contains(colName)) {
                    throw new BusinessException(ZzolBotErrorCode.SQL_COLUMN_BLOCKED,
                            "조회가 차단된 컬럼입니다: " + tableName + "." + colName);
                }
            } else {
                final boolean isBlockedAnywhere = blockedByTable.values().stream()
                        .anyMatch(blockedSet -> blockedSet.contains(colName));
                if (isBlockedAnywhere) {
                    throw new BusinessException(ZzolBotErrorCode.SQL_COLUMN_BLOCKED,
                            "조회가 차단된 컬럼입니다: " + colName);
                }
            }
        }
    }

    // 정규식 치환 대신 JSqlParser AST 조작으로 LIMIT 보정 — 문자열 치환의 부정확성을 방지
    private String applyLimitIfNeeded(String originalSql, PlainSelect plainSelect) {
        final int maxRows = properties.sql().maxRows();
        if (plainSelect.getLimit() == null) {
            final Limit limit = new Limit();
            limit.setRowCount(new LongValue(maxRows));
            plainSelect.setLimit(limit);
            return plainSelect.toString();
        }
        final String limitExpr = plainSelect.getLimit().getRowCount().toString();
        try {
            final long currentLimit = Long.parseLong(limitExpr);
            if (currentLimit > maxRows) {
                plainSelect.getLimit().setRowCount(new LongValue(maxRows));
                return plainSelect.toString();
            }
        } catch (NumberFormatException ignored) {
            // 동적 파라미터(?) 등은 그대로 통과
        }
        return originalSql;
    }
}
