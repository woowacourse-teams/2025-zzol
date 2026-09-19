package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.CitationVerifier;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 자체 모델을 호출하고 두 모델의 답을 나란히 저장한다.
 *
 * <p><b>같은 잣대로 잰다.</b> 자체 모델의 인용도 권위 모델과 같은 검증({@link CitationVerifier})을
 * 거친 값으로 기록한다. 검증 규칙이 두 벌이면 차이가 모델에서 온 것인지 채점에서 온 것인지 가릴 수
 * 없다.
 *
 * <p>접지 전 주장과 접지 후 판정을 모두 남긴다. 둘이 갈리는 건수가 곧 "인용이 어설퍼서 강등된"
 * 사례이고, 그 수가 인용 제약의 효과를 읽는 근거가 된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "zzol-bot.monitor.shadow", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ShadowAnalysisRecorder {

    private static final int ERROR_MAX_LENGTH = 500;

    private final MonitorShadowRunRepository repository;
    private final MonitorAnalysisContract contract;
    private final ShadowModelClient shadowModel;
    private final Clock clock;

    public void record(
            FiringAlert alert, List<String> logSamples, String logEnvironment, MonitorAnalysis authoritative) {
        final Instant startedAt = clock.instant();
        try {
            final String raw = shadowModel.generate(alert, logSamples, logEnvironment);
            final MonitorAnalysis analysis = contract.parse(raw, logSamples);
            final String citedLine = contract.citedLine(raw);
            repository.save(MonitorShadowRunEntity.compared(
                    startedAt,
                    alert.fingerprint(),
                    alert.alertname(),
                    logSamples.size(),
                    authoritative.evidenceFound(),
                    authoritative.summary(),
                    analysis.evidenceFound(),
                    contract.claimedEvidence(raw),
                    analysis.summary(),
                    analysis.rootCauseHypothesis(),
                    citedLine,
                    elapsedMillis(startedAt)));
            log.info(
                    "[ZzolBot] 섀도우 분석 기록. fingerprint={} 권위={} 섀도우={} 인용접지={}",
                    alert.fingerprint(),
                    authoritative.evidenceFound(),
                    analysis.evidenceFound(),
                    CitationVerifier.citedInLogs(citedLine, logSamples));
        } catch (Exception e) {
            saveFailureQuietly(alert, logSamples, authoritative, startedAt, e);
        }
    }

    /**
     * 섀도우 실패는 운영에 영향을 주지 않는다. 기록마저 실패하면 로그만 남기고 삼킨다 — 여기서
     * 예외를 올려도 받을 곳이 없고, 가상 스레드에서 죽으면 스택만 남는다.
     */
    private void saveFailureQuietly(
            FiringAlert alert,
            List<String> logSamples,
            MonitorAnalysis authoritative,
            Instant startedAt,
            Exception cause) {
        log.warn("[ZzolBot] 섀도우 분석 실패. fingerprint={}", alert.fingerprint(), cause);
        try {
            repository.save(MonitorShadowRunEntity.failed(
                    startedAt,
                    alert.fingerprint(),
                    alert.alertname(),
                    logSamples.size(),
                    authoritative.evidenceFound(),
                    authoritative.summary(),
                    elapsedMillis(startedAt),
                    truncate(cause.getMessage())));
        } catch (Exception saveFailure) {
            log.warn("[ZzolBot] 섀도우 실패 기록도 실패. fingerprint={}", alert.fingerprint(), saveFailure);
        }
    }

    private long elapsedMillis(Instant startedAt) {
        return java.time.Duration.between(startedAt, clock.instant()).toMillis();
    }

    private String truncate(String message) {
        if (message == null) {
            return "알 수 없는 오류";
        }
        return message.length() <= ERROR_MAX_LENGTH ? message : message.substring(0, ERROR_MAX_LENGTH);
    }
}
