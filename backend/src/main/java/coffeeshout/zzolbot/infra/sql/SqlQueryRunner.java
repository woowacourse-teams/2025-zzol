package coffeeshout.zzolbot.infra.sql;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.domain.ZzolBotErrorCode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class SqlQueryRunner {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate readOnlyTx;
    private final int maxExecutionTimeMs;

    public SqlQueryRunner(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager txManager,
            ZzolBotProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxExecutionTimeMs = properties.sql().queryTimeoutSeconds() * 1000;
        this.readOnlyTx = new TransactionTemplate(txManager);
        this.readOnlyTx.setReadOnly(true);
        this.readOnlyTx.setTimeout(properties.sql().queryTimeoutSeconds());
    }

    public SqlQueryResult run(String validatedSql) {
        // Spring timeout(드라이버 레벨)과 이중 방어 — DB 엔진이 직접 쿼리를 종료
        final String hintedSql = injectMaxExecutionTimeHint(validatedSql);
        try {
            final List<Map<String, Object>> rows = readOnlyTx.execute(status ->
                    jdbcTemplate.queryForList(hintedSql));
            if (rows == null) {
                return SqlQueryResult.empty();
            }
            return new SqlQueryResult(rows, false);
        } catch (InfrastructureException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[ZzolBot] sql_query 실행 실패. sql={}", hintedSql, e);
            throw new InfrastructureException(ZzolBotErrorCode.SQL_EXECUTION_FAILED,
                    "SQL 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // SELECT 키워드 대소문자 무관하게 힌트를 삽입 ("select".length() == "SELECT".length() == 6)
    private String injectMaxExecutionTimeHint(String sql) {
        return "SELECT /*+ MAX_EXECUTION_TIME(" + maxExecutionTimeMs + ") */ "
                + sql.substring("select".length());
    }
}
