package coffeeshout.minigame.infra.messaging.consumer;

import static org.mockito.BDDMockito.then;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.domain.event.RoomCreateEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameSessionInitConsumerTest {

    @Mock
    private GameSessionService gameSessionService;

    @InjectMocks
    private GameSessionInitConsumer consumer;

    @Nested
    @DisplayName("방 생성 이벤트 처리(accept)")
    class Accept {

        @Test
        @DisplayName("이벤트의 hostName으로 게스트 Gamer를 구성해 세션을 초기화한다")
        void 이벤트의_hostName으로_게스트_Gamer를_구성해_세션을_초기화한다() {
            // given
            final RoomCreateEvent event = new RoomCreateEvent("꾹이", "ABCD");

            // when
            consumer.accept(event);

            // then
            then(gameSessionService).should().initSession(new JoinCode("ABCD"), Gamer.guest("꾹이"));
        }
    }
}
