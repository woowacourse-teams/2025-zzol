package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QrCodeStatusEventHandler implements Consumer<QrCodeStatusEvent> {

    private final RoomCommandService roomCommandService;
    private final LoggingSimpMessagingTemplate messagingTemplate;

    @Override
    public void accept(QrCodeStatusEvent event) {
        LoggerFactory.getLogger(getClass()).info(
                "QR 코드 완료 이벤트 수신: eventId={}, joinCode={}, status={}",
                event.eventId(), event.joinCode(), event.status()
        );

        switch (event.status()) {
            case SUCCESS -> {
                LoggerFactory.getLogger(getClass()).info(
                        "QR 코드 완료 이벤트 처리 완료 (SUCCESS): eventId={}, joinCode={}, url={}",
                        event.eventId(), event.joinCode(), event.qrCodeUrl()
                );
                roomCommandService.assignQrCode(new JoinCode(event.joinCode()), event.qrCodeUrl());
                sendQrCode(event);
            }
            case ERROR -> {
                LoggerFactory.getLogger(getClass()).info(
                        "QR 코드 완료 이벤트 처리 완료 (ERROR): eventId={}, joinCode={}",
                        event.eventId(), event.joinCode()
                );
                roomCommandService.assignQrCodeError(new JoinCode(event.joinCode()));
                sendQrCode(event);
            }
            default -> LoggerFactory.getLogger(getClass()).warn(
                    "처리할 수 없는 QR 코드 상태: eventId={}, joinCode={}, status={}",
                    event.eventId(), event.joinCode(), event.status()
            );
        }
    }

    private void sendQrCode(QrCodeStatusEvent event) {
        final QrCodeStatusResponse response = new QrCodeStatusResponse(event.status(), event.qrCodeUrl());

        final String destination = String.format("/topic/room/%s/qr-code", event.joinCode());
        messagingTemplate.convertAndSend(destination, WebSocketResponse.success(response));

        LoggerFactory.getLogger(getClass()).debug(
                "QR 코드 이벤트 전송 완료: destination={}, status={}, url={}",
                destination, event.status(), event.qrCodeUrl()
        );
    }
}
