package coffeeshout.wormgame.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.wormgame.domain.WormGameState;
import coffeeshout.wormgame.domain.event.WormGameStateChangedEvent;
import coffeeshout.wormgame.domain.event.WormSnapshotEvent;
import coffeeshout.wormgame.domain.event.WormsMovedEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WormGameNotifierTest {

    @Mock
    private LoggingSimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WormGameNotifier notifier;

    @Test
    void 델타는_복구_저장_없이_보낸다() {
        // when
        notifier.publishDelta(new WormsMovedEvent("ABCD", 3L, 200.0, List.of()));

        // then
        then(messagingTemplate).should().convertAndSendTransient(eq("/topic/room/ABCD/worm"), any());
        then(messagingTemplate).should(never()).convertAndSend(anyString(), any());
    }

    @Test
    void 스냅샷은_복구_저장_없이_보낸다() {
        // when
        notifier.publishSnapshot(new WormSnapshotEvent("ABCD", 3L, 50L, Instant.now(), 200.0, List.of()));

        // then
        then(messagingTemplate).should().convertAndSendTransient(eq("/topic/room/ABCD/worm/snapshot"), any());
        then(messagingTemplate).should(never()).convertAndSend(anyString(), any());
    }

    @Test
    void 상태_전이는_복구_저장을_유지한다() {
        // when — DONE 중 끊긴 클라의 결과 화면 라우팅이 복구 스트림을 탄다
        notifier.publishState(new WormGameStateChangedEvent("ABCD", WormGameState.DONE));

        // then
        then(messagingTemplate).should().convertAndSend(eq("/topic/room/ABCD/worm/state"), any());
        then(messagingTemplate).should(never()).convertAndSendTransient(anyString(), any());
    }
}
