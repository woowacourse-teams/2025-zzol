package coffeeshout.global.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameDurationMetricService {

    private final MeterRegistry meterRegistry;
    private final Map<String, Sample> gameStartSamples = new ConcurrentHashMap<>();

    private Timer gameDurationTimer;

    public GameDurationMetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initializeMetrics() {
        this.gameDurationTimer = Timer.builder("game.duration.time")
                .description("게임 진행 시간 (PLAYING → DONE)")
                .register(meterRegistry);
    }

    /**
     * 게임 시작 시점 기록 (RoomState: PLAYING)
     */
    public void startGameTimer(String joinCode) {
        Sample sample = Timer.start(meterRegistry);
        gameStartSamples.put(joinCode, sample);
        log.debug("게임 시작 타이머 시작: joinCode={}", joinCode);
    }

    /**
     * 게임 종료 시점 기록 (RoomState: DONE)
     */
    public void stopGameTimer(String joinCode) {
        Sample sample = gameStartSamples.remove(joinCode);
        if (sample != null) {
            long durationNanos = sample.stop(gameDurationTimer);
            double durationSeconds = durationNanos / 1_000_000_000.0;
            log.info("게임 완료: joinCode={}, duration={}초", joinCode, durationSeconds);
        } else {
            log.warn("게임 시작 샘플을 찾을 수 없음: joinCode={}", joinCode);
        }
    }
}
