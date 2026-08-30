package coffeeshout.wormgame.infra.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.wormgame.application.WormGameService;
import coffeeshout.wormgame.domain.event.SteerCommandEvent;
import coffeeshout.wormgame.infra.messaging.consumer.SteerCommandEventConsumer;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SteerCommandEventConsumerTest {

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private WormGameService wormGameService;

    @InjectMocks
    private SteerCommandEventConsumer consumer;

    @Test
    void 세션이_없는_인스턴스는_조향을_조용히_건너뛴다() {
        // given — 브로드캐스트 스트림은 전 인스턴스가 받지만 게임은 한 인스턴스에만 있다
        given(gameSessionService.findSession(any())).willReturn(Optional.empty());

        // when
        consumer.accept(SteerCommandEvent.create("ABCD", "꾹이", 1.0, 1));

        // then
        then(wormGameService).should(never()).steer(anyString(), anyString(), anyDouble(), anyLong());
    }

    @Test
    void 세션이_있으면_조향을_서비스에_전달한다() {
        // given
        given(gameSessionService.findSession(any())).willReturn(Optional.of(mock(GameSession.class)));

        // when
        consumer.accept(SteerCommandEvent.create("ABCD", "꾹이", 1.0, 7));

        // then
        then(wormGameService).should().steer("ABCD", "꾹이", 1.0, 7);
    }
}
