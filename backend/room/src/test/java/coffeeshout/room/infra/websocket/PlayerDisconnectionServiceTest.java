package coffeeshout.room.infra.websocket;

import static org.mockito.BDDMockito.then;

import coffeeshout.room.application.service.RoomCommandService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.websocket.StompSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PlayerDisconnectionServiceTest {

    @Mock
    StompSessionManager sessionManager;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    PlayerDisconnectionService playerDisconnectionService;

    @Mock
    RoomCommandService roomCommandService;

    @Test
    void cancelReady_isReady가_false로_변경된다() {
        String playerKey = "ABC4:김철수";
        String joinCode = "ABC4";
        String playerName = "김철수";

        playerDisconnectionService.cancelReady(playerKey);

        then(roomCommandService).should().readyPlayer(new JoinCode(joinCode), new PlayerName(playerName), false);
    }
}
