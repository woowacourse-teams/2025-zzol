package coffeeshout.global.trace;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 이벤트 생성 시 TraceInfo를 추출하는 유틸리티 클래스
 */
@Slf4j
public class TraceInfoExtractor {

    private TraceInfoExtractor() {
    }

    public static TraceInfo extract() {
        try {
            final ObservationRegistry observationRegistry = ObservationRegistryProvider.getObservationRegistry();
            final Observation observation = observationRegistry.getCurrentObservation();
            final TracingContext traceContext = observation.getContext().get(TracingContext.class);
            return new TraceInfo(
                    traceContext.getSpan().context().traceId(),
                    traceContext.getSpan().context().spanId()
            );
        } catch (Exception e) {
            log.debug("Trace context 없음: {}", e.toString());
            return new TraceInfo("", "");
        }
    }
}
