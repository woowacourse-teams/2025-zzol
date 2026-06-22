package coffeeshout.zzolbot.monitor.application;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;

/**
 * Alertmanager가 발화한 firing 알림을 받아 LLM 분석(근본원인 가설·제안 조치)으로 분석한 뒤
 * 운영 채널(Slack)에 게시하는 포트. ADR-0032의 핵심 재배치 지점이다.
 *
 * <p>구현은 {@link AlertEnrichmentService}로, 기존 {@code AnomalyAnalyzer}·{@code ZzolBotSlackNotifier}를
 * 재사용해 firing 알림을 LLM 분석·영속·Slack 게시한다.
 */
public interface FiringAlertEnricher {

    void enrich(FiringAlert alert);
}
