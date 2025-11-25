package coffeeshout.global.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

@Component
public class HttpMetricService {

    private final LongAdder concurrentRequests;

    public HttpMetricService(MeterRegistry meterRegistry) {
        concurrentRequests = new LongAdder();

        // 실시간 동시 연결 수 게이지 등록
        Gauge.builder("http.concurrent.requests", concurrentRequests, LongAdder::sum)
                .description("현재 동시 HTTP 처리 수")
                .baseUnit("requests")
                .register(meterRegistry);
    }

    public void incrementConcurrentRequests() {
        concurrentRequests.increment();
    }

    public void decrementConcurrentRequests() {
        if (concurrentRequests.sum() > 0) {
            concurrentRequests.decrement();
        }
    }
}
