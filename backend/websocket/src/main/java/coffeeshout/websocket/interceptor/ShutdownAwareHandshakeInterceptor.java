package coffeeshout.websocket.interceptor;

import coffeeshout.websocket.lifecycle.WebSocketGracefulShutdownHandler;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket Handshake 시점에 서버 종료 상태를 확인하는 인터셉터
 * <p>
 * Graceful Shutdown 중일 때 새로운 WebSocket 연결을 차단하여
 * 종료 프로세스가 무한정 지연되는 것을 방지합니다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShutdownAwareHandshakeInterceptor implements HandshakeInterceptor {

    private final ObjectProvider<WebSocketGracefulShutdownHandler> shutdownHandlerProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        final WebSocketGracefulShutdownHandler shutdownHandler = shutdownHandlerProvider.getIfAvailable();

        if (shutdownHandler != null && shutdownHandler.isShuttingDown()) {
            log.warn("🚫 WebSocket Handshake 거부: 서버 Graceful Shutdown 진행 중 (from: {})",
                    Objects.toString(request.getRemoteAddress(), "unknown"));
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                              ServerHttpResponse response,
                              WebSocketHandler wsHandler,
                              Exception exception) {
        // afterHandshake는 별도 처리 불필요
    }
}
