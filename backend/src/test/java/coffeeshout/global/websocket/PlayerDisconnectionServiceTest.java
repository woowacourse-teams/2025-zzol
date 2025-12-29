package coffeeshout.global.websocket;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import coffeeshout.room.application.service.PlayerService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomCommandService;
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
    PlayerService playerService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    PlayerDisconnectionService playerDisconnectionService;

    @Mock
    RoomCommandService roomCommandService;

    @Test
    void cancelReady_isReady가_false로_변경된다() {
        // given
        String playerKey = "ABC23:김철수";
        String joinCode = "ABC23";
        String playerName = "김철수";
        
        given(sessionManager.extractJoinCode(playerKey)).willReturn(joinCode);
        given(sessionManager.extractPlayerName(playerKey)).willReturn(playerName);

        // when
        playerDisconnectionService.cancelReady(playerKey);

        // then
        then(roomCommandService).should().readyPlayer(new JoinCode(joinCode), new PlayerName(playerName), false);
    }
}
