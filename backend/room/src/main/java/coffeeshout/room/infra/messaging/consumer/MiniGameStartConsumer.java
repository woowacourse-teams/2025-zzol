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
 * 미니게임 시작 커맨드의 방 검증 단계를 담당한다(ADR-0025 결정 4 — 이벤트 분리).
 *
 * <p>시작은 방 검증(호스트·전원 준비·인원·방 상태)과 GameSession 시작의 두 책임으로 나뉜다. 검증·플레이어 명단은
 * {@code :room}의 데이터이므로 여기서 처리하고, GameSession 시작은 {@code :game}이 {@link GameStartReadyEvent}를
 * in-process 동기 수신해 수행한다. 이로써 {@code :game}이 더는 {@code RoomQueryService}로 방을 조회하지 않는다
 * ({@code :game → :room} 의존 제거).
 *
 * <p>방의 {@code PLAYING} 전이({@code markPlaying})는 여기서 하지 않는다. GameSession이 PLAYING으로 전이된
 * 직후 {@code :game}이 발행하는 {@code GameSessionStartedEvent}를 {@code RoomGameStartListener}가 받아 수행하므로,
 * 두 전이가 실패 가능 I/O보다 먼저 한 묶음으로 끝나 찢어진 상태를 막는다(검증 → 시작 → PLAYING 전이의 순서·원자성 보존).
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
    }
}
