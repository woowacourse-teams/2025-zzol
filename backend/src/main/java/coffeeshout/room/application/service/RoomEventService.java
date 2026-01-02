package coffeeshout.room.application.service;

import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.event.RouletteWinnerEvent;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.infra.persistence.RoulettePersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomEventService {

    private final ApplicationEventPublisher eventPublisher;
    private final RoulettePersistenceService roulettePersistenceService;

    public void spinRoulette(RouletteSpinEvent event) {
        log.info("JoinCode[{}] 룰렛 스핀 이벤트 처리 - 당첨자: {}", event.joinCode(), event.winner().name().value());

        final Winner winner = event.winner();
        roulettePersistenceService.saveRouletteResult(event);

        eventPublisher.publishEvent(new RouletteWinnerEvent(event.joinCode(), winner));
    }
}
