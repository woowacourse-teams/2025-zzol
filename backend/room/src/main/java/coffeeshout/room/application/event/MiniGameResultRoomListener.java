package coffeeshout.room.application.event;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.application.service.RoomCommandService;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 게임 종료 시 {@code :game}이 발행하는 {@link MiniGameFinishedEvent}를 수신해 방의 확률을 조정한다(ADR-0023 결정 5).
 *
 * <p>in-process 동기 리스너이므로 {@code publishEvent()} 반환 시점에 확률 조정이 완료된다.
 * 발행 측이 {@code finishGame()}으로 {@code roundCount}를 먼저 확정한 뒤 발행하므로 정확한 라운드 수로 조정된다.
 * {@code @Async}를 적용하지 않는다(룰렛·스코어보드 조회 타이밍 보장).
 *
 * <p>이벤트의 이름 기반 순위 맵({@code Map<String, Integer>})을 {@code :room} 경계에서 {@link PlayerName}으로 변환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameResultRoomListener {

    private final RoomCommandService roomCommandService;

    // 같은 이벤트의 저장 리스너(MiniGameResultSaveEventListener, @Order(2))보다 먼저 실행한다 —
    // 저장 리스너가 @RedisLock(waitTime=0)/DB 오류로 예외를 던져도 확률 조정·SCORE_BOARD 전이가
    // 보장돼야 한다(ADR-0023 결정 5: publishEvent 반환 시점 확률 조정 완료).
    @EventListener
    @Order(1)
    public void handle(MiniGameFinishedEvent event) {
        final Map<PlayerName, Integer> rankByPlayer = event.ranks().entrySet().stream()
                .collect(Collectors.toMap(entry -> new PlayerName(entry.getKey()), Map.Entry::getValue));

        roomCommandService.applyGameResult(new JoinCode(event.joinCode()), rankByPlayer, event.roundCount());

        log.debug("게임 결과 확률 조정 완료: joinCode={}, roundCount={}", event.joinCode(), event.roundCount());
    }
}
