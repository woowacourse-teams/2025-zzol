package coffeeshout.report.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Nested
    @DisplayName("createBugReport")
    class CreateBugReport {

        @Test
        void gameType과_joinCode가_그대로_저장된다() {
            final Report entity = Report.createBugReport(MiniGameType.CARD_GAME, "ABC12", "카드게임이 멈춰요.", FIXED_CLOCK);

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(ReportCategory.BUG);
                softly.assertThat(entity.getGameType()).isEqualTo(MiniGameType.CARD_GAME);
                softly.assertThat(entity.getJoinCode()).isEqualTo("ABC12");
                softly.assertThat(entity.getContent()).isEqualTo("카드게임이 멈춰요.");
                softly.assertThat(entity.getCreatedAt()).isEqualTo(Instant.now(FIXED_CLOCK));
            });
        }

        @Test
        void gameType과_joinCode가_null이어도_정상_생성된다() {
            final Report entity = Report.createBugReport(null, null, "게임 외 버그입니다.", FIXED_CLOCK);

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(ReportCategory.BUG);
                softly.assertThat(entity.getGameType()).isNull();
                softly.assertThat(entity.getJoinCode()).isNull();
            });
        }
    }

    @Nested
    @DisplayName("createGeneralReport")
    class CreateGeneralReport {

        @Test
        void SUGGESTION_카테고리와_content가_저장된다() {
            final Report entity = Report.createGeneralReport(ReportCategory.SUGGESTION, "건의합니다.", FIXED_CLOCK);

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(ReportCategory.SUGGESTION);
                softly.assertThat(entity.getGameType()).isNull();
                softly.assertThat(entity.getJoinCode()).isNull();
                softly.assertThat(entity.getContent()).isEqualTo("건의합니다.");
            });
        }

        @Test
        void GAME_REQUEST_카테고리와_content가_저장된다() {
            final Report entity = Report.createGeneralReport(ReportCategory.GAME_REQUEST, "새 게임 요청합니다.", FIXED_CLOCK);

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(ReportCategory.GAME_REQUEST);
                softly.assertThat(entity.getGameType()).isNull();
                softly.assertThat(entity.getJoinCode()).isNull();
            });
        }

        @Test
        void OTHER_카테고리와_content가_저장된다() {
            final Report entity = Report.createGeneralReport(ReportCategory.OTHER, "기타 내용입니다.", FIXED_CLOCK);

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(ReportCategory.OTHER);
                softly.assertThat(entity.getGameType()).isNull();
                softly.assertThat(entity.getJoinCode()).isNull();
            });
        }
    }

    @Nested
    @DisplayName("ip 필드")
    class IpField {

        @Test
        void ip를_전달하면_createBugReport에_저장된다() {
            final Report entity = Report.createBugReport(
                    MiniGameType.CARD_GAME, "ABC12", "내용", FIXED_CLOCK, null, "1.2.3.4");

            assertThat(entity.getIp()).isEqualTo("1.2.3.4");
        }

        @Test
        void ip를_전달하면_createGeneralReport에_저장된다() {
            final Report entity = Report.createGeneralReport(
                    ReportCategory.SUGGESTION, "내용", FIXED_CLOCK, null, "5.6.7.8");

            assertThat(entity.getIp()).isEqualTo("5.6.7.8");
        }

        @Test
        void ip_없이_생성하면_null이다() {
            final Report entity = Report.createBugReport(MiniGameType.CARD_GAME, "ABC12", "내용", FIXED_CLOCK);

            assertThat(entity.getIp()).isNull();
        }
    }

    @Nested
    @DisplayName("공통 필드")
    class CommonFields {

        @Test
        void content는_입력값_그대로_저장된다() {
            final String content = "테스트 내용입니다.";

            final Report entity = Report.createGeneralReport(ReportCategory.SUGGESTION, content, FIXED_CLOCK);

            assertThat(entity.getContent()).isEqualTo(content);
        }

        @Test
        void createdAt은_Clock이_반환하는_시각으로_설정된다() {
            final Report entity = Report.createGeneralReport(ReportCategory.SUGGESTION, "내용", FIXED_CLOCK);

            assertThat(entity.getCreatedAt()).isEqualTo(Instant.now(FIXED_CLOCK));
        }
    }
}
