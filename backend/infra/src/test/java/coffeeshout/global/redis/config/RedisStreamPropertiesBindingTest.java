package coffeeshout.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 공통 설정이나 스트림 키가 빠지면 기동할 때 실패하는지 확인한다.
 *
 * <p>전에는 설정이 없으면 발행 때 경고만 남기고 이벤트를 버렸다. 실패한 설정이 요청을 받기 시작한 뒤에야
 * 드러나지 않도록 바인딩 검증으로 막는다. 검증은 바인딩할 때만 돌아서 컨텍스트를 띄워 확인한다.</p>
 */
class RedisStreamPropertiesBindingTest {

    private static final String[] COMMON_SETTINGS = {
        "redis.stream.common-settings.max-length=100",
        "redis.stream.common-settings.batch-size=10",
        "redis.stream.common-settings.poll-timeout=2s"
    };
    private static final String KEY = "redis.stream.keys.room.thread-pool-name=concurrent";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(Holder.class);

    @Test
    void 공통_설정과_스트림_키가_있으면_기동한다() {
        runner.withPropertyValues(COMMON_SETTINGS)
                .withPropertyValues(KEY)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void 공통_설정이_없으면_기동에_실패한다() {
        runner.withPropertyValues(KEY).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("commonSettings");
        });
    }

    @Test
    void 스트림_키가_없으면_기동에_실패한다() {
        runner.withPropertyValues(COMMON_SETTINGS).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("keys");
        });
    }

    @EnableConfigurationProperties(RedisStreamProperties.class)
    static class Holder {}
}
