package coffeeshout.blindtimer.infra.messaging.consumer;

import coffeeshout.blindtimer.application.BlindTimerGameProgressHandler;
import coffeeshout.blindtimer.domain.event.StopCommandEvent;
import coffeeshout.global.exception.custom.BusinessException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StopCommandEventConsumer implements Consumer<StopCommandEvent> {

    private final BlindTimerGameProgressHandler progressHandler;

    @Override
    public void accept(StopCommandEvent event) {
        try {
            progressHandler.handleStop(event.joinCode(), event.playerName());
        } catch (BusinessException e) {
            log.warn("STOP 이벤트 처리 중 상태 오류: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode(), e);
        } catch (Exception e) {
            log.error("STOP 이벤트 처리 실패: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode(), e);
        }
    }
}
