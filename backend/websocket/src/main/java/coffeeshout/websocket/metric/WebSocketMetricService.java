package coffeeshout.websocket.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebSocketMetricService {

    private final MeterRegistry meterRegistry;
    // 연결이 수립된 세션 ID. 게이지가 이 집합의 크기를 읽으므로 증감을 따로 세지 않는다
    private final Set<String> establishedConnections = ConcurrentHashMap.newKeySet();
    private final Map<String, Sample> connectionSamples = new ConcurrentHashMap<>();
    // Counter 캐싱용
    private final Map<String, Counter> failedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> disconnectedCounters = new ConcurrentHashMap<>();
    // 메시지 처리 시간 측정용
    private final Map<String, Sample> inboundMessageSamples = new ConcurrentHashMap<>();
    private final Map<String, Sample> outboundMessageSamples = new ConcurrentHashMap<>();
    // 비즈니스 로직 처리 시간 측정용
    private final Map<String, Sample> businessLogicSamples = new ConcurrentHashMap<>();

    private Timer connectionEstablishmentTimer;
    private Counter inboundMessageCounter;
    private Counter outboundMessageCounter;
    private Timer inboundMessageTimer; // 큐 대기 시간 측정용
    private Timer outboundMessageTimer;
    private Timer businessLogicTimer;

    public WebSocketMetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initializeMetrics() {
        Gauge.builder("websocket.connections.current", establishedConnections, Set::size)
                .description("현재 웹소켓 연결 개수")
                .register(meterRegistry);

        this.connectionEstablishmentTimer = Timer.builder("websocket.connection.establishment.time")
                .description("웹소켓 연결 수립 시간")
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
        this.inboundMessageCounter = Counter.builder("websocket.messages.inbound.total")
                .description("인바운드 메시지 총 개수")
                .register(meterRegistry);
        this.outboundMessageCounter = Counter.builder("websocket.messages.outbound.total")
                .description("아웃바운드 메시지 총 개수")
                .register(meterRegistry);
        this.inboundMessageTimer = Timer.builder("websocket.message.inbound.time")
                .description("웹소켓 inbound 메시지 큐 대기 시간")
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
        this.outboundMessageTimer = Timer.builder("websocket.message.outbound.time")
                .description("웹소켓 outbound 메시지 처리 시간")
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
        this.businessLogicTimer = Timer.builder("websocket.message.business.logic.time")
                .description("웹소켓 inbound 메시지 비즈니스 로직 처리 시간")
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
    }

    public void startConnection(String sessionId) {
        Sample sample = Timer.start(meterRegistry);
        connectionSamples.put(sessionId, sample);
    }

    public void completeConnection(String sessionId) {
        establishedConnections.add(sessionId);
        Sample sample = connectionSamples.remove(sessionId);
        if (sample != null) {
            long durationNanos = sample.stop(connectionEstablishmentTimer);
            double durationMs = durationNanos / 1_000_000.0;
            log.info("WebSocket 연결 수립 완료: sessionId={}, duration={}ms", sessionId, durationMs);
        }
    }

    public void failConnection(String sessionId, String reason) {
        connectionSamples.remove(sessionId);

        String key = "failed." + reason;
        Counter counter = failedCounters.computeIfAbsent(key, k -> Counter.builder("websocket.connections.failed")
                .description("웹소켓 연결 실패 건수")
                .tag("reason", reason)
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * STOMP 연결이 수립된 세션인지 확인한다. CONNECT 없이 핸드셰이크만 하고 끊는 세션과 구분하는 데 쓴다.
     */
    public boolean hasEstablishedConnection(String sessionId) {
        return establishedConnections.contains(sessionId);
    }

    public void recordDisconnection(String sessionId, String reason) {
        connectionSamples.remove(sessionId);

        // 연결을 수립한 세션만 한 번씩 센다. 같은 세션에 해제 이벤트가 두 번 와도 중복 집계되지 않고,
        // CONNECT 없이 끊는 세션은 애초에 집합에 없어 게이지도 카운터도 건드리지 않는다
        if (!establishedConnections.remove(sessionId)) {
            return;
        }

        String key = "disconnected." + reason;

        Counter counter =
                disconnectedCounters.computeIfAbsent(key, k -> Counter.builder("websocket.connections.disconnected")
                        .description("웹소켓 연결 해제 건수")
                        .tag("reason", reason)
                        .register(meterRegistry));
        counter.increment();
    }

    public void incrementInboundMessage() {
        if (inboundMessageCounter != null) {
            inboundMessageCounter.increment();
        }
    }

    public void incrementOutboundMessage() {
        if (outboundMessageCounter != null) {
            outboundMessageCounter.increment();
        }
    }

    public void startInboundMessageTimer(String messageId) {
        Sample sample = Timer.start(meterRegistry);
        inboundMessageSamples.put(messageId, sample);
    }

    public void startOutboundMessageTimer(String messageId) {
        Sample sample = Timer.start(meterRegistry);
        outboundMessageSamples.put(messageId, sample);
    }

    public void stopOutboundMessageTimer(String messageId) {
        Sample sample = outboundMessageSamples.remove(messageId);
        if (sample != null) {
            sample.stop(outboundMessageTimer);
        }
    }

    // 큐 대기 시간 측정 종료
    public void stopInboundMessageTimer(String messageId) {
        Sample inboundSample = inboundMessageSamples.remove(messageId);
        if (inboundSample != null) {
            inboundSample.stop(inboundMessageTimer);
        }
    }

    // 비즈니스 로직 시간 측정 시작
    public void startBusinessTimer(String messageId) {
        Sample businessSample = Timer.start(meterRegistry);
        businessLogicSamples.put(messageId, businessSample);
    }

    // 비즈니스 로직 시간 측정 종료
    public void stopBusinessTimer(String messageId) {
        Sample sample = businessLogicSamples.remove(messageId);
        if (sample != null) {
            sample.stop(businessLogicTimer);
        }
    }
}
