package coffeeshout.global.zzolbot.infra.sql;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.ZzolBotErrorCode;
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

    public SqlQueryRunner(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager txManager,
            ZzolBotProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.readOnlyTx = new TransactionTemplate(txManager);
        this.readOnlyTx.setReadOnly(true);
        this.readOnlyTx.setTimeout(properties.sql().queryTimeoutSeconds());
    }

    public SqlQueryResult run(String validatedSql) {
        try {
            final List<Map<String, Object>> rows = readOnlyTx.execute(status ->
                    jdbcTemplate.queryForList(validatedSql));
            if (rows == null) {
                return SqlQueryResult.empty();
            }
            return new SqlQueryResult(rows, false);
        } catch (InfrastructureException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[ZzolBot] sql_query 실행 실패. sql={}", validatedSql, e);
            throw new InfrastructureException(ZzolBotErrorCode.SQL_EXECUTION_FAILED,
                    "SQL 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
