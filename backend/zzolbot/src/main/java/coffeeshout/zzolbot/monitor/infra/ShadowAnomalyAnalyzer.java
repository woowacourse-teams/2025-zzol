package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 권위 모델의 분석을 그대로 돌려주면서, 같은 입력을 자체 모델에도 통과시켜 결과만 기록한다.
 *
 * <p><b>운영 동작은 바뀌지 않는다.</b> 반환값은 권위 모델의 것이고 Slack에 게시되는 내용도 같다.
 * 자체 모델의 답은 어디에도 노출되지 않는다.
 *
 * <p><b>섀도우는 다른 스레드에서 돈다.</b> 같은 스레드에서 이어 돌리면 자체 모델의 지연이 그대로
 * Slack 게시 지연이 된다. 웹훅 수신기가 쓰는 가상 스레드 풀에 던지고 즉시 반환한다.
 *
 * <p><b>실패는 삼킨다.</b> 자체 모델이 죽거나 느려도 운영 알림은 그대로 나가야 한다. 실패 자체는
 * 기록해 가용성을 셀 수 있게 남긴다.
 *
 * <p>설정 {@code zzol-bot.monitor.shadow.enabled}가 참일 때만 빈이 만들어진다. 꺼져 있으면 이
 * 데코레이터가 존재하지 않으므로 경로가 이전과 완전히 같다.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(prefix = "zzol-bot.monitor.shadow", name = "enabled", havingValue = "true")
public class ShadowAnomalyAnalyzer implements AnomalyAnalyzer {

    private final AnomalyAnalyzer authoritative;
    private final ShadowAnalysisRecorder recorder;
    private final ExecutorService executor;

    public ShadowAnomalyAnalyzer(
            GeminiAnomalyAnalyzer authoritative,
            ShadowAnalysisRecorder recorder,
            @Qualifier("virtualThreadExecutor") ExecutorService executor) {
        this.authoritative = authoritative;
        this.recorder = recorder;
        this.executor = executor;
    }

    @Override
    public MonitorAnalysis analyze(FiringAlert alert, List<String> logSamples, String logEnvironment) {
        final MonitorAnalysis result = authoritative.analyze(alert, logSamples, logEnvironment);
        runShadow(alert, logSamples, logEnvironment, result);
        return result;
    }

    private void runShadow(
            FiringAlert alert, List<String> logSamples, String logEnvironment, MonitorAnalysis authoritativeResult) {
        try {
            final List<String> samples = List.copyOf(logSamples);
            executor.execute(() -> recorder.record(alert, samples, logEnvironment, authoritativeResult));
        } catch (Exception e) {
            // 제출 자체가 실패해도(풀 종료 등) 운영 경로는 영향받지 않는다
            log.warn("[ZzolBot] 섀도우 분석 제출 실패. fingerprint={}", alert.fingerprint(), e);
        }
    }
}
