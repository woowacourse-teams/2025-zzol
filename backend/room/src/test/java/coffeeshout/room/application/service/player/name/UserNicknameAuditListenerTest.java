package coffeeshout.room.application.service.player.name;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import coffeeshout.profanity.domain.audit.NicknameSubmittedEvent;
import coffeeshout.user.domain.event.UserNicknameRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class UserNicknameAuditListenerTest {

    private ApplicationEventPublisher eventPublisher;
    private UserNicknameAuditListener listener;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        listener = new UserNicknameAuditListener(eventPublisher);
    }

    @Nested
    class onUserNicknameRegistered_이벤트_처리 {

        @Test
        void 사용자_닉네임_등록_이벤트가_오면_NicknameSubmittedEvent를_발행한다() {
            final UserNicknameRegisteredEvent event = new UserNicknameRegisteredEvent("용감한호랑이");

            listener.onUserNicknameRegistered(event);

            then(eventPublisher).should().publishEvent(
                    argThat((Object e) -> e instanceof NicknameSubmittedEvent ne
                            && "용감한호랑이".equals(ne.nickname()))
            );
        }
    }
}
