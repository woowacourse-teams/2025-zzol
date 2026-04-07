package coffeeshout.report.infra.persistence;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.fixture.ReportFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JpaReportRepositoryTest extends ServiceTest {

    @Autowired
    private JpaReportRepository jpaReportRepository;

    @Nested
    @DisplayName("save & findById")
    class SaveAndFind {

        @Test
        void BUG_신고를_저장하고_동일한_값으로_조회된다() {
            final ReportEntity saved = jpaReportRepository.save(ReportFixture.버그_카드게임_신고());

            final Optional<ReportEntity> found = jpaReportRepository.findById(saved.getId());

            assertSoftly(softly -> {
                softly.assertThat(found).isPresent();
                softly.assertThat(found.get().getCategory()).isEqualTo(ReportCategory.BUG);
                softly.assertThat(found.get().getGameType()).isEqualTo(MiniGameType.CARD_GAME);
                softly.assertThat(found.get().getJoinCode()).isEqualTo("ABC12");
                softly.assertThat(found.get().getContent()).isEqualTo("카드게임이 멈춰요.");
                softly.assertThat(found.get().getCreatedAt()).isNotNull();
            });
        }

        @Test
        void 건의사항을_저장하면_gameType과_joinCode는_null로_조회된다() {
            final ReportEntity saved = jpaReportRepository.save(ReportFixture.건의사항());

            final Optional<ReportEntity> found = jpaReportRepository.findById(saved.getId());

            assertSoftly(softly -> {
                softly.assertThat(found).isPresent();
                softly.assertThat(found.get().getCategory()).isEqualTo(ReportCategory.SUGGESTION);
                softly.assertThat(found.get().getGameType()).isNull();
                softly.assertThat(found.get().getJoinCode()).isNull();
                softly.assertThat(found.get().getContent()).isEqualTo("새 게임을 추가해주세요.");
            });
        }
    }
}
