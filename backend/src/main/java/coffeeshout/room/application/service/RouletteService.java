package coffeeshout.room.application.service;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.repository.RouletteResultPort;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.event.PlayerNameAuditRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouletteService {

    private final RoomQueryService roomQueryService;
    private final RouletteResultPort rouletteResultPort;
    private final ApplicationEventPublisher eventPublisher;

    public RoomState showRoulette(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        room.showRoulette();
        return room.getRoomState();
    }

    public void updateRoomStatusToRoulette(String joinCode) {
        rouletteResultPort.updateRoomStatusToRoulette(joinCode);

        log.info("RoomEntity 상태 업데이트 완료: joinCode={}, status=ROULETTE", joinCode);
    }

    public void saveRouletteResult(String joinCode, Winner winner) {
        rouletteResultPort.finishRoomAndSaveResult(joinCode, winner);
        eventPublisher.publishEvent(new PlayerNameAuditRequestedEvent(winner.name().value()));
    }
}
