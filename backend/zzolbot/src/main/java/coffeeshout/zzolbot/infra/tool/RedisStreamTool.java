package coffeeshout.zzolbot.infra.tool;

import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamTool implements ZzolBotTool {

    static final String TOOL_NAME = "redis_stream_status";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisStreamProperties redisStreamProperties;

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "모든 Redis Stream 키별 현재 길이(XLEN)를 조회한다. " +
                "XLEN은 XADD MAXLEN 트리밍으로 항상 일정 수준을 유지하므로 처리 지연(lag) 지표가 아니다. " +
                "처리 지연·backpressure는 prometheus_query의 컨슈머 스레드풀 큐 깊이/lag 메트릭으로 확인한다.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of()
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> params, AskContext ctx) {
        try {
            final Map<String, Object> result = new LinkedHashMap<>();
            redisStreamProperties.keys().forEach((redisKey, config) -> {
                final Long size = redisTemplate.opsForStream().size(redisKey);
                result.put(redisKey, size != null ? size : 0L);
            });
            return ToolExecutionResult.ok(TOOL_NAME, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.warn("[ZzolBot] redis_stream_status 직렬화 실패", e);
            return ToolExecutionResult.fail(TOOL_NAME, "Redis Stream 상태 직렬화 실패");
        } catch (Exception e) {
            log.warn("[ZzolBot] redis_stream_status 조회 실패", e);
            return ToolExecutionResult.fail(TOOL_NAME, "Redis Stream 조회 실패");
        }
    }
}
