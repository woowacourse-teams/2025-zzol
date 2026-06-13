package coffeeshout.minigame.infra.messaging.consumer;

import static org.mockito.BDDMockito.then;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.RoomLifecycleEvent;
import coffeeshout.minigame.application.GameSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameSessionCleanupConsumerTest {

    @Mock
    private GameSessionService gameSessionService;

    @InjectMocks
    private GameSessionCleanupConsumer consumer;

    @Nested
    @DisplayName("방 삭제 이벤트 처리(accept)")
    class Accept {

        @Test
        @DisplayName("이벤트의 joinCode로 세션을 정리한다")
        void 이벤트의_joinCode로_세션을_정리한다() {
            // given
            final RoomLifecycleEvent.Removed event = new RoomLifecycleEvent.Removed("ABCD");

            // when
            consumer.accept(event);

            // then
            then(gameSessionService).should().deleteSession(new JoinCode("ABCD"));
        }
    }
}
