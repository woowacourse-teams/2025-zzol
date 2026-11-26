package coffeeshout.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.redis.config.RedisStreamProperties.CommonSettings;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import coffeeshout.global.redis.config.RedisStreamProperties.ThreadPoolConfig;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RedisStreamPropertiesTest {

    private static final ThreadPoolConfig POOL = new ThreadPoolConfig(1, 1, 128);

    @Nested
    class 리스너를_생성하는_스트림_설정을_검증할_때 {

        @Test
        void 스레드풀을_지정하지_않으면_예외가_발생한다() {
            assertThatThrownBy(() -> new StreamConfig(null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("반드시 지정");
        }

        @Test
        void 공유_풀과_전용_풀을_동시에_지정하면_예외가_발생한다() {
            assertThatThrownBy(() -> new StreamConfig("concurrent", POOL, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("동시에 지정");
        }

        @Test
        void listener_enabled를_명시하지_않으면_리스너를_생성한다() {
            final StreamConfig config = new StreamConfig("concurrent", null, null, null, null, null);

            assertThat(config.isListenerEnabled()).isTrue();
        }
    }

    @Nested
    class 리스너를_생성하지_않는_스트림_설정을_검증할_때 {

        @Test
        void 스레드풀_없이_선언할_수_있다() {
            // 컨슈머 그룹 전용 작업 큐 스트림은 소비 스레드풀이 필요 없다(#1610)
            assertThatCode(() -> new StreamConfig(null, null, 10000, null, null, false))
                    .doesNotThrowAnyException();
        }

        @Test
        void 공유_풀을_지정하면_예외가_발생한다() {
            assertThatThrownBy(() -> new StreamConfig("concurrent", null, null, null, null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("스레드풀을 지정할 수 없습니다");
        }

        @Test
        void 전용_풀을_지정하면_예외가_발생한다() {
            assertThatThrownBy(() -> new StreamConfig(null, POOL, null, null, null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("스레드풀을 지정할 수 없습니다");
        }
    }

    @Nested
    class 스트림의_최대_길이를_구할_때 {

        private final CommonSettings common = new CommonSettings(100, 10, Duration.ofSeconds(2), Duration.ofSeconds(5));
        private final RedisStreamProperties properties = new RedisStreamProperties(
                common,
                Map.of(),
                Map.of(
                        "room", new StreamConfig("concurrent", null, null, null, null, null),
                        "room:join", new StreamConfig("concurrent", null, 500, null, null, null)));

        @Test
        void 스트림에_따로_정하지_않으면_공통_설정을_따른다() {
            assertThat(properties.maxLength("room")).isEqualTo(100);
        }

        @Test
        void 스트림에_정한_값이_있으면_그_값을_쓴다() {
            assertThat(properties.maxLength("room:join")).isEqualTo(500);
        }

        @Test
        void 설정에_없는_키면_예외가_발생한다() {
            assertThatThrownBy(() -> properties.maxLength("unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 키입니다: unknown");
        }
    }
}
