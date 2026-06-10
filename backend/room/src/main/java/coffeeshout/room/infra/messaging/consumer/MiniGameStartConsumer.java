package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.GameStartReadyEvent;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 미니게임 시작 커맨드의 방 검증 단계를 담당한다(ADR-0023 결정 4 — 이벤트 분리).
 *
 * <p>시작은 방 검증(호스트·전원 준비·인원·방 상태)과 GameSession 시작의 두 책임으로 나뉜다. 검증·플레이어 명단·방
 * 상태는 {@code :room}의 데이터이므로 여기서 처리하고, GameSession 시작은 {@code :game}이 {@link GameStartReadyEvent}를
 * in-process 동기 수신해 수행한다. 이로써 {@code :game}이 더는 {@code RoomQueryService}로 방을 조회하지 않는다
 * ({@code :game → :room} 의존 제거).
 *
 * <p>처리 순서가 불변식이다: 검증(읽기 전용) → {@code GameStartReadyEvent} 발행(동기 — 내부에서 GameSession 시작)
 * → {@code markPlaying}. 동기 발행이라 {@code startGame} 실패(예: 빈 대기열)가 예외로 전파되면 {@code markPlaying}이
 * 스킵돼 방이 READY로 남는다(검증 → 시작 → PLAYING 전이의 원자성·순서 보존).
 */
@Component
@RequiredArgsConstructor
public class MiniGameStartConsumer implements Consumer<StartMiniGameCommandEvent> {

    private final RoomQueryService roomQueryService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void accept(StartMiniGameCommandEvent event) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(event.joinCode()));
        room.validateStartable(event.hostName());

        eventPublisher.publishEvent(new GameStartReadyEvent(
                event.eventId(), event.joinCode(), event.hostName(), room.getGamers()));

        room.markPlaying();
    }
}
