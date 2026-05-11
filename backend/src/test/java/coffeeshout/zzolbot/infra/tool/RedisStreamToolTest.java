package coffeeshout.zzolbot.infra.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import coffeeshout.global.redis.stream.StreamKey;
import java.util.List;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

@ExtendWith(MockitoExtension.class)
class RedisStreamToolTest {

    private static final AskContext CTX = AskContext.stamp("test", List.of(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private RedisStreamTool redisStreamTool;

    @BeforeEach
    void setUp() {
        redisStreamTool = new RedisStreamTool(redisTemplate, new ObjectMapper());
    }

    @Nested
    class execute_메서드 {

        @Test
        @SuppressWarnings("unchecked")
        void 모든_스트림_키별_메시지_수를_반환한다() {
            final StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
            given(redisTemplate.opsForStream()).willReturn(streamOps);
            given(streamOps.size(anyString())).willReturn(5L);

            final ToolExecutionResult result = redisStreamTool.execute(Map.of(), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isTrue();
                softly.assertThat(result.toolName()).isEqualTo(RedisStreamTool.TOOL_NAME);
                for (StreamKey key : StreamKey.values()) {
                    softly.assertThat(result.content()).contains(key.getRedisKey());
                }
            });
        }

        @Test
        @SuppressWarnings("unchecked")
        void Redis_조회_실패_시_실패_결과를_반환한다() {
            final StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
            given(redisTemplate.opsForStream()).willReturn(streamOps);
            given(streamOps.size(anyString())).willThrow(new RuntimeException("Redis 연결 실패"));

            final ToolExecutionResult result = redisStreamTool.execute(Map.of(), CTX);

            assertThat(result.success()).isFalse();
        }
    }
}
