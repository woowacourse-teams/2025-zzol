package coffeeshout.nunchi.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.nunchi.domain.NunchiGame;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link NunchiService} 단위 테스트. 게임 조회·닉네임 해석 후 오케스트레이터에 위임하는지 검증한다.
 * 도메인 판정·타이머·브로드캐스트는 {@link NunchiFlowOrchestrator}의 책임이라 여기선 위임만 본다.
 */
class NunchiServiceTest {

    private static final JoinCode JOIN_CODE = new JoinCode("ABCD");
    private static final Instant T0 = Instant.parse("2026-06-21T00:00:00Z");

    private final Gamer 일 = Gamer.of("일", null);
    private final Gamer 이 = Gamer.of("이", null);

    private GameSessionService gameSessionService;
    private NunchiFlowOrchestrator flowOrchestrator;
    private NunchiService service;
    private NunchiGame game;

    @BeforeEach
    void setUp() {
        gameSessionService = mock(GameSessionService.class);
        flowOrchestrator = mock(NunchiFlowOrchestrator.class);
        service = new NunchiService(gameSessionService, flowOrchestrator);

        game = new NunchiGame(300L);
        game.setUp(List.of(일, 이));

        final GameSession session = mock(GameSession.class);
        when(gameSessionService.getSession(JOIN_CODE)).thenReturn(session);
        when(session.findCompletedGame(MiniGameType.NUNCHI_GAME)).thenReturn(game);
    }

    @Test
    void start는_게임을_조회해_Flow를_시작한다() {
        service.start(JOIN_CODE.getValue(), "일");

        verify(flowOrchestrator).startFlow(eq(game), any(JoinCode.class));
    }

    @Test
    void handlePress는_닉네임을_원본_Gamer로_해석해_Flow에_위임한다() {
        service.handlePress(JOIN_CODE.getValue(), "일", T0);

        // 새 Gamer가 아니라 setUp으로 주입된 원본 인스턴스를 넘겨야 점수맵 키와 매칭된다
        verify(flowOrchestrator).handlePress(eq(game), any(JoinCode.class), eq(일), eq(T0));
    }

    @Test
    void handlePress는_방내_없는_플레이어면_BusinessException을_전파하고_Flow에_위임하지_않는다() {
        // findByName이 도메인에서 예외를 던지므로 오케스트레이터까지 가지 않는다(컨슈머가 warn으로 흡수)
        assertThatThrownBy(() -> service.handlePress(JOIN_CODE.getValue(), "없는사람", T0))
                .isInstanceOf(BusinessException.class);

        verify(flowOrchestrator, never()).handlePress(any(), any(), any(), any());
    }

    @Test
    void getMiniGameType은_NUNCHI_GAME이다() {
        org.assertj.core.api.Assertions.assertThat(service.getMiniGameType())
                .isEqualTo(MiniGameType.NUNCHI_GAME);
    }
}
