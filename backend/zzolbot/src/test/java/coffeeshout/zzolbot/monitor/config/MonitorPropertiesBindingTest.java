package coffeeshout.zzolbot.monitor.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 설정이 실제로 바인딩되는지 확인한다.
 *
 * <p><b>왜 컨텍스트를 띄우나.</b> 이 record에 편의 생성자를 하나 더 뒀다가 앱이 기동하지 않는
 * 일을 겪었다. Spring Boot는 비-private 생성자가 정확히 하나일 때만 바인딩 생성자를 추론하고,
 * 둘이 되면 JavaBean 바인딩으로 떨어져 기본 생성자가 없다는 이유로 실패한다. 순수 단위 테스트는
 * 생성자를 직접 부르므로 이 실패를 잡지 못한다.
 */
class MonitorPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(Holder.class);

    @Nested
    class 설정이_바인딩된다 {

        @Test
        void shadow_블록이_없으면_꺼진_것으로_본다() {
            runner.withPropertyValues(
                            "zzol-bot.monitor.enabled=true",
                            "zzol-bot.monitor.error-log-window-minutes=30",
                            "zzol-bot.monitor.enrich-cooldown-minutes=240")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        final MonitorProperties properties = context.getBean(MonitorProperties.class);
                        SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(properties.enabled()).isTrue();
                            softly.assertThat(properties.shadow().enabled()).isFalse();
                        });
                    });
        }

        @Test
        void shadow_블록을_주면_그대로_읽는다() {
            runner.withPropertyValues(
                            "zzol-bot.monitor.enabled=true",
                            "zzol-bot.monitor.error-log-window-minutes=30",
                            "zzol-bot.monitor.enrich-cooldown-minutes=240",
                            "zzol-bot.monitor.shadow.enabled=true",
                            "zzol-bot.monitor.shadow.base-url=http://llama:8081",
                            "zzol-bot.monitor.shadow.connect-timeout-millis=1000",
                            "zzol-bot.monitor.shadow.read-timeout-millis=60000",
                            "zzol-bot.monitor.shadow.max-tokens=500")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        final MonitorProperties.ShadowProperties shadow =
                                context.getBean(MonitorProperties.class).shadow();
                        SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(shadow.enabled()).isTrue();
                            softly.assertThat(shadow.baseUrl()).isEqualTo("http://llama:8081");
                            softly.assertThat(shadow.readTimeout().toMillis()).isEqualTo(60000L);
                        });
                    });
        }
    }

    @EnableConfigurationProperties(MonitorProperties.class)
    static class Holder {}
}
