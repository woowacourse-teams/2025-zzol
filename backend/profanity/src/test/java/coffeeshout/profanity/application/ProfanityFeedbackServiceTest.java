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

        @Test
        void 정상으로_확정하면_ALLOWED가_되고_표본_표시는_남는다() {
            final NicknameAudit sample = unreviewedSample("용감한호랑이");
            given(auditRepository.findById(1L)).willReturn(Optional.of(sample));

            service.allowSample(1L);

            assertThat(sample.getStatus()).isEqualTo(NicknameAuditStatus.ALLOWED);
            assertThat(sample.isReviewSample()).isTrue();
            then(profanityWordManagementService).should().operatorAllow("용감한호랑이");
        }

        @Test
        void 미탐으로_확정하면_BLOCKED가_되고_사전에_오른다() {
            final NicknameAudit sample = unreviewedSample("욕설닉네임");
            given(auditRepository.findById(1L)).willReturn(Optional.of(sample));
            given(profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.MANUAL))
                    .willReturn(true);

            service.blockSample(1L);

            assertThat(sample.getStatus()).isEqualTo(NicknameAuditStatus.BLOCKED);
            then(eventPublisher).should().publishEvent(any(ProfanityWordBlockedEvent.class));
        }

        @Test
        void 표본이_아닌_행은_거절한다() {
            final NicknameAudit flagged = auditEntityWith("걸린닉네임");
            flagged.complete(NicknameAuditStatus.FLAGGED, AiConfidence.of(0.9), "욕설");
            given(auditRepository.findById(1L)).willReturn(Optional.of(flagged));

            assertCoffeeShoutException(() -> service.allowSample(1L), NicknameAuditErrorCode.NOT_UNREVIEWED_SAMPLE);
            assertThat(flagged.getStatus()).isEqualTo(NicknameAuditStatus.FLAGGED);
            then(feedbackRepository).should(never()).save(any());
        }

        @Test
        void 이미_결정한_표본은_거절한다() {
            final NicknameAudit decided = unreviewedSample("용감한호랑이");
            decided.updateStatus(NicknameAuditStatus.ALLOWED);
            given(auditRepository.findById(1L)).willReturn(Optional.of(decided));

            assertCoffeeShoutException(() -> service.blockSample(1L), NicknameAuditErrorCode.NOT_UNREVIEWED_SAMPLE);
            then(profanityWordManagementService).should(never()).add(any(), any(), any());
        }

        @Test
        void 존재하지_않는_표본은_예외가_발생한다() {
            given(auditRepository.findById(999L)).willReturn(Optional.empty());

            assertCoffeeShoutException(() -> service.allowSample(999L), NicknameAuditErrorCode.AUDIT_NOT_FOUND);
        }

        private NicknameAudit unreviewedSample(String nickname) {
            final NicknameAudit audit = auditEntityWith(nickname);
            audit.complete(NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반");
            audit.markReviewSample();
            return audit;
        }
    }

    private NicknameAudit auditEntityWith(String nickname) {
        return new NicknameAudit(nickname);
    }
}
