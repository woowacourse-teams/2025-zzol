package coffeeshout.minigame.infra.messaging.consumer;

import static org.mockito.BDDMockito.then;

import coffeeshout.gamecommon.Gamer;
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
class GameSessionHostChangeConsumerTest {

    @Mock
    private GameSessionService gameSessionService;

    @InjectMocks
    private GameSessionHostChangeConsumer consumer;

    @Nested
    @DisplayName("호스트 승계 이벤트 처리(accept)")
    class Accept {

        @Test
        @DisplayName("이벤트의 joinCode와 새 호스트 이름으로 세션 호스트를 갱신한다")
        void 이벤트의_joinCode와_새_호스트_이름으로_세션_호스트를_갱신한다() {
            // given — 호스트 검증은 이름 기준이므로 newHostName만으로 Gamer.guest를 구성한다
            final RoomLifecycleEvent.HostChanged event = new RoomLifecycleEvent.HostChanged("ABCD", "새호스트");

            // when
            consumer.accept(event);

            // then
            then(gameSessionService).should().updateHost(new JoinCode("ABCD"), Gamer.guest("새호스트"));
        }
    }
}
