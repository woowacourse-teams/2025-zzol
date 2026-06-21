package coffeeshout.nunchi.ui;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.nunchi.ui.response.NunchiResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 눈치게임 결과 조회 REST(ADR-0031 N7). 공유 {@code /minigames/ranks·scores}는 tier가 없어 충돌·미입력
 * 구분이 불가하므로 전용 엔드포인트로 {@code {playerName, rank, tier}}를 rank 오름차순으로 반환한다.
 * result와 scores를 같은 게임 인스턴스에서 함께 읽어 rank·tier가 어긋나지 않게 한다.
 */
@RestController
@RequiredArgsConstructor
public class NunchiRestController {

    private final GameSessionService gameSessionService;

    @GetMapping("/minigames/nunchi/result")
    public ResponseEntity<NunchiResultResponse> getResult(@RequestParam String joinCode) {
        final Playable game = gameSessionService.getSession(new JoinCode(joinCode))
                .findCompletedGame(MiniGameType.NUNCHI_GAME);
        return ResponseEntity.ok(NunchiResultResponse.of(game.getResult(), game.getScores()));
    }
}
