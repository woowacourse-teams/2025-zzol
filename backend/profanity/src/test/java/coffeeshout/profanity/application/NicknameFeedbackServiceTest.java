package coffeeshout.profanity.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.global.event.ProfanityWordBlockedEvent;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityErrorCode;

import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditEntity;
import coffeeshout.profanity.infra.persistence.audit.NicknameFeedbackEntity;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class NicknameFeedbackServiceTest {

    private NicknameAuditRepository auditRepository;
    private NicknameFeedbackRepository feedbackRepository;
    private ProfanityWordManagementService profanityWordManagementService;
    private ApplicationEventPublisher eventPublisher;
    private NicknameFeedbackService service;

    @BeforeEach
    void setUp() {
        auditRepository = mock(NicknameAuditRepository.class);
        feedbackRepository = mock(NicknameFeedbackRepository.class);
        profanityWordManagementService = mock(ProfanityWordManagementService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new NicknameFeedbackService(auditRepository, feedbackRepository, profanityWordManagementService, eventPublisher);
    }

    @Nested
    class allow_허용_처리 {

        @Test
        void 검열_항목을_ALLOWED로_변경하고_피드백을_저장한다() {
            final NicknameAuditEntity audit = auditEntityWith("용감한호랑이");
            given(auditRepository.findById(1L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.allow(1L);

            assertThat(audit.getStatus()).isEqualTo(NicknameAuditStatus.ALLOWED);
            then(feedbackRepository).should().save(any(NicknameFeedbackEntity.class));
        }

        @Test
        void 비속어_목록에_없는_닉네임도_허용_처리된다() {
            final NicknameAuditEntity audit = auditEntityWith("용감한호랑이");
            given(auditRepository.findById(1L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            willThrow(new BusinessException(ProfanityErrorCode.WORD_NOT_FOUND, "없음"))
                    .given(profanityWordManagementService).deactivate("용감한호랑이");

            service.allow(1L);

            assertThat(audit.getStatus()).isEqualTo(NicknameAuditStatus.ALLOWED);
        }

        @Test
        void 존재하지_않는_검열_항목은_예외가_발생한다() {
            given(auditRepository.findById(999L)).willReturn(Optional.empty());

            assertCoffeeShoutException(
                    () -> service.allow(999L),
                    NicknameAuditErrorCode.AUDIT_NOT_FOUND
            );
        }
    }

    @Nested
    class block_차단_처리 {

        @Test
        void 검열_항목을_BLOCKED로_변경하고_비속어로_등록한다() {
            final NicknameAuditEntity audit = auditEntityWith("욕설닉네임");
            given(auditRepository.findById(1L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.block(1L);

            assertThat(audit.getStatus()).isEqualTo(NicknameAuditStatus.BLOCKED);
            then(profanityWordManagementService).should().add("욕설닉네임", Language.KOREAN, WordSource.MANUAL);
            then(eventPublisher).should().publishEvent(any(ProfanityWordBlockedEvent.class));
        }

        @Test
        void 차단_시_피드백이_저장된다() {
            final NicknameAuditEntity audit = auditEntityWith("욕설닉네임");
            given(auditRepository.findById(1L)).willReturn(Optional.of(audit));
            given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.block(1L);

            then(feedbackRepository).should().save(any(NicknameFeedbackEntity.class));
        }

        @Test
        void 존재하지_않는_검열_항목은_예외가_발생한다() {
            given(auditRepository.findById(999L)).willReturn(Optional.empty());

            assertCoffeeShoutException(
                    () -> service.block(999L),
                    NicknameAuditErrorCode.AUDIT_NOT_FOUND
            );
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

    private NicknameAuditEntity auditEntityWith(String nickname) {
        return new NicknameAuditEntity(nickname);
    }
}
