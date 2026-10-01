package coffeeshout.profanity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * cron 형식을 바인딩 시점에 잡는지 검증한다.
 *
 * <p>{@code @NotBlank}는 공백 여부만 본다. 형식이 틀린 값을 여기서 막지 못하면 바인딩은 통과하고
 * 스케줄러 후처리기가 기동을 실패시킨다. 그 시점 예외에는 어느 프로퍼티가 원인인지 나오지 않는다.
 */
class NicknameAuditPropertiesTest {

    @Nested
    class cron_형식_검증 {

        @Test
        void 필드가_모자란_cron은_바인딩에서_거부된다() {
            assertThatThrownBy(() -> NicknameAuditPropertiesFixture.주기("*/5 * * * *"))
                    .as("필드 5개짜리 표준 crontab 표기다. Spring은 6개를 요구한다.")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 여섯_필드_cron은_그대로_통과한다() {
            assertThat(NicknameAuditPropertiesFixture.주기("0 */5 * * * *").cron())
                    .isEqualTo("0 */5 * * * *");
        }
    }
}
