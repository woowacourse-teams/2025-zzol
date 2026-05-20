package coffeeshout.room.application.service.player.name;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.fixture.PlayerNameAuditFixture;
import coffeeshout.ServiceTest;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.infra.nickname.event.ProfanityWordAllowedEvent;
import coffeeshout.room.infra.nickname.event.ProfanityWordBlockedEvent;
import coffeeshout.room.infra.nickname.persistence.CustomProfanityJpaRepository;
import coffeeshout.room.infra.nickname.persistence.PlayerNameAuditEntity;
import coffeeshout.room.infra.nickname.persistence.PlayerNameAuditJpaRepository;
import coffeeshout.room.infra.nickname.persistence.PlayerNameFeedbackJpaRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

class PlayerNameFeedbackServiceTest extends ServiceTest {

    @Autowired
    PlayerNameFeedbackService feedbackService;
    @Autowired
    PlayerNameAuditJpaRepository auditRepository;
    @Autowired
    PlayerNameFeedbackJpaRepository feedbackRepository;
    @Autowired
    CustomProfanityJpaRepository customProfanityRepository;

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
        class AI가_자동_차단한_FLAGGED_항목인_경우 {

            @Test
            void AI_AUDIT_엔트리가_삭제된다() {
                customProfanityRepository.insertIgnore("욕설닉네임", "AI_AUDIT");
                PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.allow(audit.getId());

                assertThat(customProfanityRepository.existsByWord("욕설닉네임")).isFalse();
            }

            @Test
            void ProfanityWordAllowedEvent를_발행한다() {
                customProfanityRepository.insertIgnore("욕설닉네임", "AI_AUDIT");
                PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.allow(audit.getId());

                verify(eventPublisher).publishEvent(new ProfanityWordAllowedEvent("욕설닉네임"));
            }
        }

        @Nested
        class OPERATOR_MANUAL_엔트리가_존재하는_경우 {

            @Test
            void 운영자가_직접_등록한_금칙어는_삭제하지_않는다() {
                customProfanityRepository.insertIgnore("욕설닉네임", "OPERATOR_MANUAL");
                PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.allow(audit.getId());

                assertThat(customProfanityRepository.existsByWord("욕설닉네임")).isTrue();
            }

            @Test
            void custom_profanity가_삭제되지_않으므로_이벤트를_발행하지_않는다() {
                customProfanityRepository.insertIgnore("욕설닉네임", "OPERATOR_MANUAL");
                PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.allow(audit.getId());

                verify(eventPublisher, never()).publishEvent(any());
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
        void 커스텀_비속어로_등록되고_ProfanityWordBlockedEvent를_발행한다() {
            PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

            feedbackService.block(audit.getId());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(customProfanityRepository.existsByWord("욕설닉네임")).isTrue();
                softly.assertThatCode(() -> verify(eventPublisher).publishEvent(new ProfanityWordBlockedEvent("욕설닉네임")))
                        .doesNotThrowAnyException();
            });
        }

        @Nested
        class AI_AUDIT으로_이미_등록된_경우 {

            @Test
            void source가_OPERATOR_MANUAL로_업그레이드되고_이벤트를_발행한다() {
                customProfanityRepository.insertIgnore("욕설닉네임", "AI_AUDIT");
                PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.block(audit.getId());

                // OPERATOR_MANUAL로 업그레이드됐으면 allow() 호출 시 삭제되지 않아야 함
                PlayerNameAuditEntity audit2 = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));
                feedbackService.allow(audit2.getId());

                assertThat(customProfanityRepository.existsByWord("욕설닉네임")).isTrue();
            }
        }

        @Nested
        class OPERATOR_MANUAL로_이미_등록된_경우 {

            @Test
            void 중복_등록하지_않는다() {
                customProfanityRepository.insertIgnore("욕설닉네임", "OPERATOR_MANUAL");
                PlayerNameAuditEntity audit = auditRepository.save(PlayerNameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.block(audit.getId());

                assertThat(customProfanityRepository.findWords(Pageable.unpaged()).getContent())
                        .containsExactly("욕설닉네임");
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
