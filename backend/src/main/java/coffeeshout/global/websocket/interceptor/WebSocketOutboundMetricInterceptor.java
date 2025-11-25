package coffeeshout.global.websocket.interceptor;

import coffeeshout.global.metric.WebSocketMetricService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketOutboundMetricInterceptor implements ExecutorChannelInterceptor {

    private final WebSocketMetricService webSocketMetricService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final var type = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
        if (SimpMessageType.HEARTBEAT.equals(type)) {
            return message;
        }

        if (SimpMessageType.MESSAGE.equals(type)) {
            try {
                final MessageHeaderAccessor mutableAccessor = SimpMessageHeaderAccessor.getMutableAccessor(message);
                mutableAccessor.setHeader("messageId", UUID.randomUUID().toString());
                Message<?> headerMessage = MessageBuilder.createMessage(
                        message.getPayload(),
                        mutableAccessor.getMessageHeaders()
                );
                webSocketMetricService.startOutboundMessageTimer(headerMessage.getHeaders().get("messageId")
                        .toString());
                return headerMessage;
            } catch (Exception e) {
                log.error("WebSocket 아웃바운드 메시지 전송 시간 측정 시작 중 에러", e);
            }
        }

        return message;
    }

    @Override
    public void afterMessageHandled(
            Message<?> message,
            MessageChannel channel,
            MessageHandler handler,
            Exception exception
    ) {
        final var type = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
        if (SimpMessageType.HEARTBEAT.equals(type)) {
            return;
        }

        if (SimpMessageType.MESSAGE.equals(type)) {
            try {
                final String timingId = message.getHeaders().get("messageId").toString();
                webSocketMetricService.stopOutboundMessageTimer(timingId);
            } catch (Exception e) {
                log.error("WebSocket 아웃바운드 메시지 전송 시간 측정 완료 중 에러", e);
            }
        }

        try {
            webSocketMetricService.incrementOutboundMessage();
        } catch (Exception e) {
            log.error("WebSocket 아웃바운드 메트릭 수집 중 에러", e);
        }
    }
}
