package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트 환경에서 Gemini 호출 없이 고정 분석을 반환한다.
 */
@Component
@Profile("test")
public class NoOpAnomalyAnalyzer implements AnomalyAnalyzer {

    @Override
    public MonitorAnalysis analyze(MonitorSnapshot snapshot, AnomalyVerdict verdict, List<String> logSamples) {
        return new MonitorAnalysis("NoOp 분석", "", List.of());
    }
}
