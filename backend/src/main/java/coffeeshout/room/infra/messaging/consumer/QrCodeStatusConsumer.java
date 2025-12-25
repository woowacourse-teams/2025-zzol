package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomEventService;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QrCodeStatusConsumer implements Consumer<QrCodeStatusEvent> {

    private final RoomEventService roomEventService;

    @Override
    public void accept(QrCodeStatusEvent event) {
        roomEventService.handleQrCodeStatus(event);
    }

}
