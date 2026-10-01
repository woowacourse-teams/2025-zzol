package coffeeshout.wormgame.infra.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.wormgame.application.WormGameService;
import coffeeshout.wormgame.domain.event.WormSnapshotEvent;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@ExtendWith(MockitoExtension.class)
class WormSnapshotSubscriptionHandlerTest {

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private WormGameService wormGameService;

    @Mock
    private LoggingSimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WormSnapshotSubscriptionHandler handler;

    private final Principal player = () -> "ABCD:꾹이";

    private SessionSubscribeEvent subscribe(String destination, Principal principal) {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionId("session-1");
        accessor.setUser(principal);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionSubscribeEvent(this, message, principal);
    }

    @Test
    void 델타_토픽을_구독하면_그_세션에만_스냅샷을_보낸다() {
        // given
        given(gameSessionService.findSession(any())).willReturn(Optional.of(mock(GameSession.class)));
        given(wormGameService.snapshot("ABCD"))
                .willReturn(new WormSnapshotEvent("ABCD", 10L, 50L, Instant.now(), 200.0, List.of()));

        // when
        handler.handleSubscribe(subscribe("/topic/room/ABCD/worm", player));

        // then
        then(messagingTemplate).should().convertAndSendToUser(eq("ABCD:꾹이"), eq("/queue/worm/snapshot"), any());
    }

    @Test
    void 다른_토픽_구독은_무시한다() {
        // when
        handler.handleSubscribe(subscribe("/topic/room/ABCD/worm/state", player));

        // then
        then(messagingTemplate).should(never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void 세션이_없는_인스턴스는_보내지_않는다() {
        // given
        given(gameSessionService.findSession(any())).willReturn(Optional.empty());

        // when
        handler.handleSubscribe(subscribe("/topic/room/ABCD/worm", player));

        // then
        then(messagingTemplate).should(never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}
