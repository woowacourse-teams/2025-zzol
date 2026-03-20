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
