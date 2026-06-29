package coffeeshout.nunchi.application;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.nunchi.domain.NunchiGame;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 눈치게임 애플리케이션 서비스(ADR-0031). 게임 조회·참가자 해석만 하고, 도메인 호출·타이머·브로드캐스트의
 * 직렬화는 {@link NunchiFlowOrchestrator}에 위임한다(press와 타이머 콜백이 서로 다른 풀에서 같은
 * 게임을 변경하므로 joinCode별 단일 락으로 묶어야 한다 — 컨슈머 단일스레드만으로는 부족하다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NunchiService implements MiniGameService {

    private final GameSessionService gameSessionService;
    private final NunchiFlowOrchestrator flowOrchestrator;

    @Override
    public void start(String joinCode, String hostName) {
        final JoinCode code = new JoinCode(joinCode);
        final NunchiGame game = getGame(code);
        flowOrchestrator.startFlow(game, code);
        log.info("눈치게임 시작: joinCode={}", joinCode);
    }

    /**
     * press 입력 처리(컨슈머 경유). 권위 시각 {@code at}으로 도메인 판정을 하되, 실제 도메인 호출과
     * 타이머 재예약·브로드캐스트는 오케스트레이터가 joinCode 락 아래에서 수행한다.
     */
    public void handlePress(String joinCode, String playerName, Instant at) {
        final JoinCode code = new JoinCode(joinCode);
        final NunchiGame game = getGame(code);
        final Gamer gamer = game.findByName(playerName);
        flowOrchestrator.handlePress(game, code, gamer, at);
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.NUNCHI_GAME;
    }

    private NunchiGame getGame(JoinCode joinCode) {
        return (NunchiGame) gameSessionService.getSession(joinCode)
                .findCompletedGame(MiniGameType.NUNCHI_GAME);
    }
}
