package coffeeshout.speedtouch.infra.messaging.consumer;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.speedtouch.application.SpeedTouchGameProgressHandler;
import coffeeshout.speedtouch.domain.event.TouchProgressCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TouchProgressEventConsumer implements Consumer<TouchProgressCommandEvent> {

    private final SpeedTouchGameProgressHandler progressHandler;

    @Override
    public void accept(TouchProgressCommandEvent event) {
        try {
            progressHandler.handleTouch(
                    event.joinCode(),
                    event.playerName(),
                    event.touchedNumber()
            );
        } catch (BusinessException e) {
            log.warn("터치 이벤트 처리 중 상태 오류: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode(), e);
        } catch (Exception e) {
            log.error("터치 이벤트 처리 실패: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode(), e);
        }
    }
}
