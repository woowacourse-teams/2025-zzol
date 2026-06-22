package coffeeshout.zzolbot.monitor.application;

/**
 * Alertmanager가 발화한 firing 알림을 받아 LLM 분석(근본원인 가설·제안 조치)으로 보강한 뒤
 * 운영 채널(Slack)에 게시하는 포트. ADR-0032의 핵심 재배치 지점이다.
 *
 * <p>골격 단계 구현은 {@code LoggingFiringAlertEnricher}(로깅 전용)다. 결정 승인 후
 * 기존 {@code AnomalyAnalyzer}·{@code ZzolBotSlackNotifier}를 재사용하는 실제 보강 구현으로 교체한다.
 */
public interface FiringAlertEnricher {

    void enrich(FiringAlert alert);
}
