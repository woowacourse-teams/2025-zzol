package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QrCodeStatusConsumer implements Consumer<QrCodeStatusEvent> {

    private final RoomService roomService;

    @Override
    public void accept(QrCodeStatusEvent event) {
        roomService.handleQrCodeStatus(event);
    }

}
