package coffeeshout.global.zzolbot.infra.tool;

import coffeeshout.global.exception.custom.CoffeeShoutException;
import coffeeshout.global.zzolbot.domain.AskContext;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import coffeeshout.global.zzolbot.infra.sql.SqlQueryResult;
import coffeeshout.global.zzolbot.infra.sql.SqlQueryRunner;
import coffeeshout.global.zzolbot.infra.sql.SqlQueryValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqlQueryTool implements ZzolBotTool {

    static final String TOOL_NAME = "sql_query"; // 같은 패키지 테스트에서 참조

    private final SqlQueryValidator validator;
    private final SqlQueryRunner runner;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "운영 통계 조회용 read-only SQL 도구. " +
                "회원·방·미니게임 등 운영 데이터를 집계할 때 사용한다. " +
                "단일 SELECT 문만 허용되며, LIMIT가 없으면 자동으로 제한된다. " +
                "와일드카드(*)는 사용할 수 없으며 컬럼을 직접 명시해야 한다.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "sql", Map.of(
                                "type", "string",
                                "description", "실행할 단일 SELECT SQL. 와일드카드(*) 사용 금지, 컬럼 명시 필수. LIMIT 미포함 시 자동 제한됨."
                        )
                ),
                "required", List.of("sql")
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> params, AskContext ctx) {
        if (!(params.get("sql") instanceof String rawSql)) {
            return ToolExecutionResult.fail(TOOL_NAME, "sql 파라미터가 누락되었거나 올바르지 않습니다.");
        }
        try {
            final String validatedSql = validator.validate(rawSql);
            final SqlQueryResult result = runner.run(validatedSql);
            return ToolExecutionResult.ok(TOOL_NAME, serialize(validatedSql, result));
        } catch (CoffeeShoutException e) {
            return ToolExecutionResult.fail(TOOL_NAME, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("[ZzolBot] sql_query 결과 직렬화 실패", e);
            return ToolExecutionResult.fail(TOOL_NAME, "결과 직렬화에 실패했습니다.");
        }
    }

    private String serialize(String executedSql, SqlQueryResult result) throws JsonProcessingException {
        final Map<String, Object> response = new LinkedHashMap<>();
        response.put("executedSql", executedSql);
        response.put("rowCount", result.rows().size());
        response.put("truncated", result.truncated());
        response.put("rows", result.rows());
        return objectMapper.writeValueAsString(response);
    }
}
