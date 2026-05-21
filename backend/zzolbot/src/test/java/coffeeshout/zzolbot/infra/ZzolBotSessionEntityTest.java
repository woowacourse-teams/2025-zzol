package coffeeshout.zzolbot.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.domain.ZzolBotFeedback;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ZzolBotSessionEntityTest {

    @Nested
    class create_팩토리_메서드 {

        @Test
        void 질문_답변_어드민_정보로_세션을_생성한다() {
            final ZzolBotSessionEntity entity = ZzolBotSessionEntity.create(
                    "A4BX 방 상태 알려줘", "PLAYING 상태입니다.", "admin"
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(entity.getQuestion()).isEqualTo("A4BX 방 상태 알려줘");
                softly.assertThat(entity.getAnswer()).isEqualTo("PLAYING 상태입니다.");
                softly.assertThat(entity.getAdminUsername()).isEqualTo("admin");
                softly.assertThat(entity.getFeedback()).isNull();
                softly.assertThat(entity.getCreatedAt()).isNotNull();
            });
        }
    }

    @Nested
    class applyFeedback_메서드 {

        @Test
        void GOOD_피드백을_적용한다() {
            final ZzolBotSessionEntity entity = ZzolBotSessionEntity.create("질문", "답변", "admin");

            entity.applyFeedback(ZzolBotFeedback.GOOD);

            assertThat(entity.getFeedback()).isEqualTo(ZzolBotFeedback.GOOD);
        }

        @Test
        void BAD_피드백을_적용한다() {
            final ZzolBotSessionEntity entity = ZzolBotSessionEntity.create("질문", "답변", "admin");

            entity.applyFeedback(ZzolBotFeedback.BAD);

            assertThat(entity.getFeedback()).isEqualTo(ZzolBotFeedback.BAD);
        }

        @Test
        void 피드백을_덮어쓸_수_있다() {
            final ZzolBotSessionEntity entity = ZzolBotSessionEntity.create("질문", "답변", "admin");
            entity.applyFeedback(ZzolBotFeedback.BAD);

            entity.applyFeedback(ZzolBotFeedback.GOOD);

            assertThat(entity.getFeedback()).isEqualTo(ZzolBotFeedback.GOOD);
        }
    }
}
