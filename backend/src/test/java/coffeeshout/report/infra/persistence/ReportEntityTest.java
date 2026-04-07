package coffeeshout.report.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportEntityTest {

    @Nested
    @DisplayName("BUG 카테고리 생성")
    class BugCategory {

        @Test
        void gameType과_joinCode가_그대로_저장된다() {
            final ReportEntity entity = ReportEntity.create(
                    ReportCategory.BUG, MiniGameType.CARD_GAME, "ABC12", "카드게임이 멈춰요."
            );

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(ReportCategory.BUG);
                softly.assertThat(entity.getGameType()).isEqualTo(MiniGameType.CARD_GAME);
                softly.assertThat(entity.getJoinCode()).isEqualTo("ABC12");
                softly.assertThat(entity.getContent()).isEqualTo("카드게임이 멈춰요.");
                softly.assertThat(entity.getCreatedAt()).isNotNull();
            });
        }

        @Test
        void gameType과_joinCode가_null이어도_정상_생성된다() {
            final ReportEntity entity = ReportEntity.create(
                    ReportCategory.BUG, null, null, "게임 외 버그입니다."
            );

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(ReportCategory.BUG);
                softly.assertThat(entity.getGameType()).isNull();
                softly.assertThat(entity.getJoinCode()).isNull();
            });
        }
    }

    @Nested
    @DisplayName("BUG 외 카테고리 생성")
    class NonBugCategory {

        @Test
        void SUGGESTION_카테고리는_gameType과_joinCode를_null로_저장한다() {
            final ReportEntity entity = ReportEntity.create(
                    ReportCategory.SUGGESTION, MiniGameType.CARD_GAME, "ABC12", "건의합니다."
            );

            assertSoftly(softly -> {
                softly.assertThat(entity.getCategory()).isEqualTo(ReportCategory.SUGGESTION);
                softly.assertThat(entity.getGameType()).isNull();
                softly.assertThat(entity.getJoinCode()).isNull();
            });
        }

        @Test
        void GAME_REQUEST_카테고리는_gameType과_joinCode를_null로_저장한다() {
            final ReportEntity entity = ReportEntity.create(
                    ReportCategory.GAME_REQUEST, MiniGameType.RACING_GAME, "XYZ99", "새 게임 요청합니다."
            );

            assertSoftly(softly -> {
                softly.assertThat(entity.getGameType()).isNull();
                softly.assertThat(entity.getJoinCode()).isNull();
            });
        }

        @Test
        void OTHER_카테고리는_gameType과_joinCode를_null로_저장한다() {
            final ReportEntity entity = ReportEntity.create(
                    ReportCategory.OTHER, MiniGameType.SPEED_TOUCH, "ZZZ11", "기타 내용입니다."
            );

            assertSoftly(softly -> {
                softly.assertThat(entity.getGameType()).isNull();
                softly.assertThat(entity.getJoinCode()).isNull();
            });
        }
    }

    @Nested
    @DisplayName("공통 필드")
    class CommonFields {

        @Test
        void content는_입력값_그대로_저장된다() {
            final String content = "테스트 내용입니다.";

            final ReportEntity entity = ReportEntity.create(
                    ReportCategory.SUGGESTION, null, null, content
            );

            assertThat(entity.getContent()).isEqualTo(content);
        }

        @Test
        void createdAt은_null이_아니다() {
            final ReportEntity entity = ReportEntity.create(
                    ReportCategory.SUGGESTION, null, null, "내용"
            );

            assertThat(entity.getCreatedAt()).isNotNull();
        }
    }
}
