package coffeeshout.room.application.service.player.name;

import static coffeeshout.fixture.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

import coffeeshout.fixture.PlayerNameAuditFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.global.event.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.ProfanityWordManagementService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.infra.persistence.ProfanityWordJpaRepository;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditJpaRepository;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackJpaRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PlayerNameFeedbackServiceTest extends ServiceTest {

    @Autowired
    PlayerNameFeedbackService feedbackService;
    @Autowired
    PlayerNameAuditJpaRepository auditRepository;
    @Autowired
    PlayerNameFeedbackJpaRepository feedbackRepository;
    @Autowired
    ProfanityWordJpaRepository profanityWordJpaRepository;
    @Autowired
    ProfanityWordManagementService profanityWordManagementService;

    @Nested
    class allow_처리 {

        @Test
        void 검열_항목의_상태가_ALLOWED로_변경되고_피드백이_저장된다() {
            PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("용감한호랑이"));

            feedbackService.allow(audit.getId());

            SoftAssertions.assertSoftly(softly -> {
                PlayerNameAuditEntity updated = auditRepository.findById(audit.getId()).orElseThrow();
                softly.assertThat(updated.getStatus()).isEqualTo(PlayerNameAuditStatus.ALLOWED);
                softly.assertThat(feedbackRepository.count()).isEqualTo(1);
            });
        }

        @Nested
        class 비속어_목록에_등록된_닉네임인_경우 {

            @Test
            void 비속어가_비활성화된다() {
                profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.AI_FLAGGED);
                PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.allow(audit.getId());

                assertThat(profanityWordJpaRepository.findAllActive())
                        .noneMatch(e -> e.getWord().equals("욕설닉네임"));
            }
        }

        @Nested
        class 비속어_목록에_없는_닉네임인_경우 {

            @Test
            void 예외_없이_ALLOWED로_처리된다() {
                PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("용감한호랑이"));

                assertThatCode(() -> feedbackService.allow(audit.getId())).doesNotThrowAnyException();
            }
        }

        @Test
        void 존재하지_않는_auditId이면_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> feedbackService.allow(999L),
                    RoomErrorCode.NO_EXIST_PLAYER_NAME_AUDIT
            );
        }
    }

    @Nested
    class block_처리 {

        @Test
        void 검열_항목의_상태가_BLOCKED로_변경되고_피드백이_저장된다() {
            PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

            feedbackService.block(audit.getId());

            SoftAssertions.assertSoftly(softly -> {
                PlayerNameAuditEntity updated = auditRepository.findById(audit.getId()).orElseThrow();
                softly.assertThat(updated.getStatus()).isEqualTo(PlayerNameAuditStatus.BLOCKED);
                softly.assertThat(feedbackRepository.count()).isEqualTo(1);
            });
        }

        @Test
        void 비속어_목록에_등록되고_ProfanityWordBlockedEvent를_발행한다() {
            PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

            feedbackService.block(audit.getId());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(profanityWordJpaRepository.existsByWord("욕설닉네임")).isTrue();
                softly.assertThatCode(() -> verify(eventPublisher).publishEvent(new ProfanityWordBlockedEvent("욕설닉네임")))
                        .doesNotThrowAnyException();
            });
        }

        @Nested
        class 이미_비속어_목록에_등록된_경우 {

            @Test
            void 중복_등록하지_않는다() {
                profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.MANUAL);
                PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.block(audit.getId());

                assertThat(profanityWordJpaRepository.findAll().stream()
                        .filter(e -> e.getWord().equals("욕설닉네임"))
                        .count()).isEqualTo(1);
            }
        }

        @Test
        void 존재하지_않는_auditId이면_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> feedbackService.block(999L),
                    RoomErrorCode.NO_EXIST_PLAYER_NAME_AUDIT
            );
        }
    }
}
