package coffeeshout.global.zzolbot.infra.tool;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
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

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "모든 Redis Stream 키별 현재 메시지 적재량(XLEN)을 조회한다. " +
                "특정 방 처리 당시 스트림이 밀려 있었는지 진단할 때 사용한다.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of()
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> params) {
        try {
            final Map<String, Object> result = new LinkedHashMap<>();
            Arrays.stream(StreamKey.values()).forEach(key -> {
                final Long size = redisTemplate.opsForStream().size(key.getRedisKey());
                result.put(key.getRedisKey(), size != null ? size : 0L);
            });
            return ToolExecutionResult.ok(TOOL_NAME, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.warn("[ZzolBot] redis_stream_status 직렬화 실패", e);
            return ToolExecutionResult.fail(TOOL_NAME, "Redis Stream 상태 직렬화 실패");
        } catch (Exception e) {
            log.warn("[ZzolBot] redis_stream_status 조회 실패", e);
            return ToolExecutionResult.fail(TOOL_NAME, "Redis Stream 조회 실패: " + e.getMessage());
        }
    }
}
