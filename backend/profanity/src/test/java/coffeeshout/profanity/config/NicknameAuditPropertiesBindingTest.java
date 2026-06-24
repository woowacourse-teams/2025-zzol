package coffeeshout.profanity.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NicknameAuditPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    void models_리스트가_순서대로_바인딩된다() {
        runner.withPropertyValues(
                        "nickname-audit.gemini-api-key=k",
                        "nickname-audit.models[0]=gemini-3-flash",
                        "nickname-audit.models[1]=gemini-3.5-flash",
                        "nickname-audit.models[2]=gemini-2.5-flash",
                        "nickname-audit.flagged-threshold=0.85",
                        "nickname-audit.batch-size=100",
                        "nickname-audit.feedback-injection-threshold=20")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(NicknameAuditProperties.class).models())
                            .containsExactly("gemini-3-flash", "gemini-3.5-flash", "gemini-2.5-flash");
                });
    }

    @Test
    void models가_비어있으면_NotEmpty_검증으로_기동이_실패한다() {
        runner.withPropertyValues(
                        "nickname-audit.gemini-api-key=k",
                        "nickname-audit.flagged-threshold=0.85",
                        "nickname-audit.batch-size=100",
                        "nickname-audit.feedback-injection-threshold=20")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(NicknameAuditProperties.class)
    static class PropertiesConfig {
    }
}
