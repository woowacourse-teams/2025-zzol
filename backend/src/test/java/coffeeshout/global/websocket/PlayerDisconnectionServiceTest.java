package coffeeshout.global.websocket;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import coffeeshout.room.application.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PlayerDisconnectionServiceTest {

    @Mock
    private StompSessionManager sessionManager;

    @Mock
    private PlayerService playerService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PlayerDisconnectionService playerDisconnectionService;

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
        then(playerService).should().changePlayerReadyState(joinCode, playerName, false);
    }
}
