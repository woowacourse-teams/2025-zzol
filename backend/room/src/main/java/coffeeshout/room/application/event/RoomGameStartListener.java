package coffeeshout.room.application.event;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.GameSessionStartedEvent;
import coffeeshout.room.application.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * GameSession이 {@code PLAYING}으로 전이됐을 때({@link GameSessionStartedEvent}) 방도 {@code PLAYING}으로
 * 동기화한다(ADR-0023 결정 4).
 *
 * <p>{@code :game}이 {@code startGame} 직후, 실패 가능 I/O(게임 시작·결과 영속)보다 <b>먼저</b> in-process 동기로
 * 발행하므로 이 리스너의 {@code markPlaying}이 그 I/O보다 먼저 완료된다. 따라서 이후 I/O가 실패해도 GameSession·Room이
 * 모두 PLAYING으로 일관되게 남는다(찢어진 상태 방지). 게임 시작 검증·플레이어 명단 전달은 {@code MiniGameStartConsumer}가
 * 담당한다. {@code MiniGameResultRoomListener}(결정 5)와 함께 {@code :room}의 게임 생명주기 in-process 리스너다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomGameStartListener {

    private final RoomQueryService roomQueryService;

    @EventListener
    public void handle(GameSessionStartedEvent event) {
        roomQueryService.getByJoinCode(new JoinCode(event.joinCode())).markPlaying();
        log.debug("게임 시작에 따른 방 PLAYING 전이 완료: joinCode={}", event.joinCode());
    }
}
