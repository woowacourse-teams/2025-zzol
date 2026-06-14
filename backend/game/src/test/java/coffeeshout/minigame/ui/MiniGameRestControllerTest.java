package coffeeshout.minigame.ui;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.MiniGameType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MiniGameRestController")
class MiniGameRestControllerTest {

    private static final String JOIN_CODE = "ABCD";

    @Mock
    private GameSessionService gameSessionService;

    @InjectMocks
    private MiniGameRestController controller;

    @Nested
    @DisplayName("방 존재 검증이 필요한 조회는 (RoomQueryService→GameSession 1:1 대응으로 전환)")
    class 방_존재_검증 {

        @Test
        @DisplayName("세션이 없으면(=방 없음) NOT_EXIST(404)로 거부한다")
        void 세션이_없으면_404로_거부한다() {
            given(gameSessionService.findSession(new JoinCode(JOIN_CODE))).willReturn(Optional.empty());

            assertCoffeeShoutException(
                    () -> controller.getSelectedMiniGames(JOIN_CODE),
                    GlobalErrorCode.NOT_EXIST
            );
        }

        @Test
        @DisplayName("세션이 있으면 선택된 미니게임 타입 목록을 반환한다")
        void 세션이_있으면_선택_목록을_반환한다() {
            given(gameSessionService.findSession(new JoinCode(JOIN_CODE)))
                    .willReturn(Optional.of(mock(GameSession.class)));
            given(gameSessionService.getSelectedTypes(new JoinCode(JOIN_CODE)))
                    .willReturn(List.of(MiniGameType.CARD_GAME));

            assertThat(controller.getSelectedMiniGames(JOIN_CODE).getBody())
                    .containsExactly(MiniGameType.CARD_GAME);
        }

        @Test
        @DisplayName("remaining 조회도 세션이 없으면 NOT_EXIST(404)로 거부한다")
        void remaining_조회도_세션이_없으면_404로_거부한다() {
            given(gameSessionService.findSession(new JoinCode(JOIN_CODE))).willReturn(Optional.empty());

            assertCoffeeShoutException(
                    () -> controller.getRemainingMiniGames(JOIN_CODE),
                    GlobalErrorCode.NOT_EXIST
            );
        }
    }
}
