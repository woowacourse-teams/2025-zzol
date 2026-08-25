package coffeeshout.wormgame.infra.messaging.consumer;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.wormgame.application.WormGameService;
import coffeeshout.wormgame.domain.event.SteerCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SteerCommandEventConsumer implements Consumer<SteerCommandEvent> {

    private final GameSessionService gameSessionService;
    private final WormGameService wormGameService;

    @Override
    public void accept(SteerCommandEvent event) {
        // 브로드캐스트 스트림이라 모든 인스턴스가 받는다. 세션이 없는(비소유) 인스턴스는 조용히 건너뛴다 —
        // 8인×10Hz 조향을 예외·로그로 처리하면 초당 80건 스팸이 된다.
        if (gameSessionService.findSession(new JoinCode(event.joinCode())).isEmpty()) {
            return;
        }
        try {
            wormGameService.steer(event.joinCode(), event.playerName(), event.angle(), event.seq());
        } catch (BusinessException e) {
            log.warn("조향 처리 중 상태 오류: eventId={}, joinCode={}", event.eventId(), event.joinCode(), e);
        } catch (Exception e) {
            log.error("조향 처리 실패: eventId={}, joinCode={}", event.eventId(), event.joinCode(), e);
        }
    }
}
