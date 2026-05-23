package coffeeshout.websocket.config;

import coffeeshout.websocket.interceptor.ShutdownAwareHandshakeInterceptor;
import coffeeshout.websocket.interceptor.WebSocketInboundMetricInterceptor;
import coffeeshout.websocket.interceptor.WebSocketOutboundMetricInterceptor;
import coffeeshout.websocket.interceptor.WebSocketRateLimitInterceptor;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketMessageBrokerConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * room 모듈에서 @Component로 등록된 StompPrincipalInterceptor.
     * :websocket이 :room을 모르므로 ChannelInterceptor 타입 + @Qualifier로 식별.
     */
    private final ChannelInterceptor stompPrincipalInterceptor;
    private final WebSocketRateLimitInterceptor webSocketRateLimitInterceptor;
    private final WebSocketInboundMetricInterceptor webSocketInboundMetricInterceptor;
    private final WebSocketOutboundMetricInterceptor webSocketOutboundMetricInterceptor;
    private final ShutdownAwareHandshakeInterceptor shutdownAwareHandshakeInterceptor;
    private final ObservationRegistry observationRegistry;
    private final ContextSnapshotFactory snapshotFactory;

    public WebSocketMessageBrokerConfig(
            @Qualifier("stompPrincipalInterceptor") ChannelInterceptor stompPrincipalInterceptor,
            WebSocketRateLimitInterceptor webSocketRateLimitInterceptor,
            WebSocketInboundMetricInterceptor webSocketInboundMetricInterceptor,
            WebSocketOutboundMetricInterceptor webSocketOutboundMetricInterceptor,
            ShutdownAwareHandshakeInterceptor shutdownAwareHandshakeInterceptor,
            ObservationRegistry observationRegistry,
            ContextSnapshotFactory snapshotFactory
    ) {
        this.stompPrincipalInterceptor = stompPrincipalInterceptor;
        this.webSocketRateLimitInterceptor = webSocketRateLimitInterceptor;
        this.webSocketInboundMetricInterceptor = webSocketInboundMetricInterceptor;
        this.webSocketOutboundMetricInterceptor = webSocketOutboundMetricInterceptor;
        this.shutdownAwareHandshakeInterceptor = shutdownAwareHandshakeInterceptor;
        this.observationRegistry = observationRegistry;
        this.snapshotFactory = snapshotFactory;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("wss-heartbeat-thread-");
        heartbeatScheduler.initialize();

        config.enableSimpleBroker("/topic/", "/queue/")
                .setHeartbeatValue(new long[]{4000, 4000})
                .setTaskScheduler(heartbeatScheduler);

        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(shutdownAwareHandshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration
                .interceptors(stompPrincipalInterceptor, webSocketRateLimitInterceptor, webSocketInboundMetricInterceptor)
                .taskExecutor()
                .corePoolSize(32)
                .maxPoolSize(32)
                .queueCapacity(2048)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketOutboundMetricInterceptor);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("outbound-");
        executor.setTaskDecorator(runnable -> {
            final ContextSnapshot snapshot = snapshotFactory.captureAll();
            return snapshot.wrap(() -> {
                final Observation parent = observationRegistry.getCurrentObservation();
                if (parent != null) {
                    Observation.createNotStarted("websocket.outbound", observationRegistry)
                            .parentObservation(parent)
                            .lowCardinalityKeyValue("thread", Thread.currentThread().getName())
                            .observeChecked(runnable::run);
                } else {
                    runnable.run();
                }
            });
        });
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(4096);
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        registration.taskExecutor(executor);
    }
}
