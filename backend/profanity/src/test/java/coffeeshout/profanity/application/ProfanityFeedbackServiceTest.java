package coffeeshout.profanity.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameFeedback;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ProfanityFeedbackServiceTest {

    private NicknameAuditRepository auditRepository;
    private NicknameFeedbackRepository feedbackRepository;
    private ProfanityWordManagementService profanityWordManagementService;
    private ApplicationEventPublisher eventPublisher;
    private ProfanityFeedbackService service;

    @BeforeEach
    void setUp() {
        auditRepository = mock(NicknameAuditRepository.class);
        feedbackRepository = mock(NicknameFeedbackRepository.class);
        profanityWordManagementService = mock(ProfanityWordManagementService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ProfanityFeedbackService(
                auditRepository, feedbackRepository, profanityWordManagementService, eventPublisher);
    }

    @Nested
    class allow_허용_처리 {

        @Test
        void 검열_항목을_ALLOWED로_변경하고_피드백을_저장한다() {
            final NicknameAudit audit = auditEntityWith("용감한호랑이");
            given(auditRepository.findById(1L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.allow(1L);

            assertThat(audit.getStatus()).isEqualTo(NicknameAuditStatus.ALLOWED);
            then(feedbackRepository).should().save(any(NicknameFeedback.class));
        }

        @Test
        void 허용_시_source_무관하게_operatorAllow가_호출된다() {
            final NicknameAudit audit = auditEntityWith("욕설닉네임");
            given(auditRepository.findById(1L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.allow(1L);

            then(profanityWordManagementService).should().operatorAllow("욕설닉네임");
        }

        @Test
        void 존재하지_않는_검열_항목은_예외가_발생한다() {
            given(auditRepository.findById(999L)).willReturn(Optional.empty());

            assertCoffeeShoutException(() -> service.allow(999L), NicknameAuditErrorCode.AUDIT_NOT_FOUND);
        }
    }

    @Nested
    class block_차단_처리 {

        @Test
        void 검열_항목을_BLOCKED로_변경하고_비속어로_등록한다() {
            final NicknameAudit audit = auditEntityWith("욕설닉네임");
            given(auditRepository.findById(1L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.MANUAL))
                    .willReturn(true);

            service.block(1L);

            assertThat(audit.getStatus()).isEqualTo(NicknameAuditStatus.BLOCKED);
            then(profanityWordManagementService).should().add("욕설닉네임", Language.KOREAN, WordSource.MANUAL);
            then(eventPublisher).should().publishEvent(any(ProfanityWordBlockedEvent.class));
        }

        @Test
        void 이미_등록된_단어_차단_시_이벤트를_발행하지_않는다() {
            final NicknameAudit audit = auditEntityWith("욕설닉네임");
            given(auditRepository.findById(1L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.MANUAL))
                    .willReturn(false);

            service.block(1L);

            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        void 차단_시_피드백이_저장된다() {
            final NicknameAudit audit = auditEntityWith("욕설닉네임");
            given(auditRepository.findById(1L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.block(1L);

            then(feedbackRepository).should().save(any(NicknameFeedback.class));
        }

        @Test
        void 영어_닉네임은_ENGLISH_언어로_등록된다() {
            final NicknameAudit audit = auditEntityWith("badword");
            given(auditRepository.findById(2L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(profanityWordManagementService.add("badword", Language.ENGLISH, WordSource.MANUAL))
                    .willReturn(true);

            service.block(2L);

            then(profanityWordManagementService).should().add("badword", Language.ENGLISH, WordSource.MANUAL);
        }

        @Test
        void 존재하지_않는_검열_항목은_예외가_발생한다() {
            given(auditRepository.findById(999L)).willReturn(Optional.empty());

            assertCoffeeShoutException(() -> service.block(999L), NicknameAuditErrorCode.AUDIT_NOT_FOUND);
        }

        @Test
        void 예외_발생_시_비속어_등록이_수행되지_않는다() {
            given(auditRepository.findById(999L)).willReturn(Optional.empty());

            try {
                service.block(999L);
            } catch (Exception ignored) {
            }

            then(profanityWordManagementService).should(never()).add(any(), any(), any());
        }
    }

    /**
     * 표본 결정은 허용·차단 경로를 그대로 타되, 검토 대기 중인 표본에만 열려 있어야 한다.
     * FLAGGED 행이 넘어오면 오탐으로, 이미 결정한 표본이 넘어오면 피드백이 두 번 쌓인다.
     */
    @Nested
    class 표본_결정 {

        /**
         * 맞게 통과시킨 CLEAN은 교정이 아니다. 피드백을 남기면 프롬프트의 최근 예시 20건이 "정상" 확인으로
         * 밀려나고, 운영자 허용 단어로 올리면 닉네임 전체가 사전에 들어가 트라이가 다시 빌드된다.
         */
        @Test
        void 정상은_상태만_ALLOWED로_바꾸고_피드백도_허용_단어도_남기지_않는다() {
            given(auditRepository.claimUnreviewedSample(1L, NicknameAuditStatus.ALLOWED))
                    .willReturn(1);

            service.allowSample(1L);

            then(auditRepository).should().claimUnreviewedSample(1L, NicknameAuditStatus.ALLOWED);
            then(feedbackRepository).should(never()).save(any());
            then(profanityWordManagementService).should(never()).operatorAllow(any());
        }

        @Test
        void 미탐은_BLOCKED로_선점한_뒤_피드백과_사전_등록까지_차단_경로를_탄다() {
            final NicknameAudit claimed = sample("욕설닉네임", NicknameAuditStatus.BLOCKED);
            given(auditRepository.claimUnreviewedSample(1L, NicknameAuditStatus.BLOCKED))
                    .willReturn(1);
            given(auditRepository.findById(1L)).willReturn(Optional.of(claimed));
            given(profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.MANUAL))
                    .willReturn(true);

            service.blockSample(1L);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(claimed.getStatus()).isEqualTo(NicknameAuditStatus.BLOCKED);
                then(feedbackRepository).should().save(any(NicknameFeedback.class));
                then(eventPublisher).should().publishEvent(any(ProfanityWordBlockedEvent.class));
            });
        }

        /**
         * 두 운영자가 같은 표본을 동시에 누르면 읽고 나서 확인하는 가드는 둘 다 통과한다.
         * 조건부 UPDATE가 0행이면 이미 다른 쪽이 가져간 것이라 아무것도 남기지 않는다.
         */
        @Test
        void 선점에_실패하면_거절하고_아무것도_남기지_않는다() {
            given(auditRepository.claimUnreviewedSample(1L, NicknameAuditStatus.BLOCKED))
                    .willReturn(0);
            given(auditRepository.findById(1L)).willReturn(Optional.of(sample("욕설닉네임", NicknameAuditStatus.ALLOWED)));

            assertCoffeeShoutException(() -> service.blockSample(1L), NicknameAuditErrorCode.NOT_UNREVIEWED_SAMPLE);
            then(feedbackRepository).should(never()).save(any());
            then(profanityWordManagementService).should(never()).add(any(), any(), any());
        }

        @Test
        void 존재하지_않는_표본은_예외가_발생한다() {
            given(auditRepository.claimUnreviewedSample(999L, NicknameAuditStatus.ALLOWED))
                    .willReturn(0);
            given(auditRepository.findById(999L)).willReturn(Optional.empty());

            assertCoffeeShoutException(() -> service.allowSample(999L), NicknameAuditErrorCode.AUDIT_NOT_FOUND);
        }

        private NicknameAudit sample(String nickname, NicknameAuditStatus status) {
            final NicknameAudit audit = auditEntityWith(nickname);
            audit.complete(status, AiConfidence.of(0.99), "일반");
            audit.markReviewSample();
            return audit;
        }
    }

    private NicknameAudit auditEntityWith(String nickname) {
        return new NicknameAudit(nickname);
    }
}
