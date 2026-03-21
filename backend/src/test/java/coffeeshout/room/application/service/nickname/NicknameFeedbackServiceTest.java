package coffeeshout.room.application.service.nickname;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.NicknameAuditFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.audit.NicknameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityEntity;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityJpaRepository;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditEntity;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditJpaRepository;
import coffeeshout.room.infra.persistence.nickname.NicknameFeedbackJpaRepository;
import com.vane.badwordfiltering.BadWordFiltering;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

class NicknameFeedbackServiceTest extends ServiceTest {

    @Autowired NicknameFeedbackService feedbackService;
    @Autowired NicknameAuditJpaRepository auditRepository;
    @Autowired NicknameFeedbackJpaRepository feedbackRepository;
    @Autowired CustomProfanityJpaRepository customProfanityRepository;
    @Autowired BadWordFiltering badWordFiltering;

    @Nested
    class allow_처리 {

        @Test
        void 검열_항목의_상태가_ALLOWED로_변경되고_피드백이_저장된다() {
            NicknameAuditEntity audit = auditRepository.save(NicknameAuditFixture.검열완료_FLAGGED("용감한호랑이"));

            feedbackService.allow(audit.getId());

            SoftAssertions.assertSoftly(softly -> {
                NicknameAuditEntity updated = auditRepository.findById(audit.getId()).orElseThrow();
                softly.assertThat(updated.getStatus()).isEqualTo(NicknameAuditStatus.ALLOWED);
                softly.assertThat(feedbackRepository.count()).isEqualTo(1);
            });
        }

        @Nested
        class 자동_차단된_FLAGGED_항목인_경우 {

            @Test
            void custom_profanity에서_삭제된다() {
                customProfanityRepository.save(
                        new CustomProfanityEntity("욕설닉네임", CustomProfanityEntity.Source.AI_AUDIT)
                );
                NicknameAuditEntity audit = auditRepository.save(NicknameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.allow(audit.getId());

                assertThat(customProfanityRepository.existsByWord("욕설닉네임")).isFalse();
            }

            @Test
            void BadWordFiltering에서_즉시_제거된다() {
                badWordFiltering.add("욕설닉네임");
                customProfanityRepository.save(
                        new CustomProfanityEntity("욕설닉네임", CustomProfanityEntity.Source.AI_AUDIT)
                );
                NicknameAuditEntity audit = auditRepository.save(NicknameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.allow(audit.getId());

                assertThat(badWordFiltering.check("욕설닉네임")).isFalse();
            }
        }

        @Test
        void 존재하지_않는_auditId이면_예외를_던진다() {
            assertThatThrownBy(() -> feedbackService.allow(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    class block_처리 {

        @Test
        void 검열_항목의_상태가_BLOCKED로_변경되고_피드백이_저장된다() {
            NicknameAuditEntity audit = auditRepository.save(NicknameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

            feedbackService.block(audit.getId());

            SoftAssertions.assertSoftly(softly -> {
                NicknameAuditEntity updated = auditRepository.findById(audit.getId()).orElseThrow();
                softly.assertThat(updated.getStatus()).isEqualTo(NicknameAuditStatus.BLOCKED);
                softly.assertThat(feedbackRepository.count()).isEqualTo(1);
            });
        }

        @Nested
        class 커스텀_비속어_미등록_상태인_경우 {

            @Test
            void 커스텀_비속어로_등록된다() {
                NicknameAuditEntity audit = auditRepository.save(NicknameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.block(audit.getId());

                assertThat(customProfanityRepository.existsByWord("욕설닉네임")).isTrue();
            }
        }

        @Nested
        class 커스텀_비속어_이미_등록된_경우 {

            @Test
            void 중복_등록하지_않는다() {
                customProfanityRepository.save(
                        new CustomProfanityEntity("욕설닉네임", CustomProfanityEntity.Source.OPERATOR_MANUAL)
                );
                NicknameAuditEntity audit = auditRepository.save(NicknameAuditFixture.검열완료_FLAGGED("욕설닉네임"));

                feedbackService.block(audit.getId());

                assertThat(customProfanityRepository.findWords(Pageable.unpaged()))
                        .containsExactly("욕설닉네임");
            }
        }

        @Test
        void 존재하지_않는_auditId이면_예외를_던진다() {
            assertThatThrownBy(() -> feedbackService.block(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }
}
