package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;

/**
 * 이상 신호를 LLM으로 요약·근본원인 분석한다. 이상으로 판정되고 예산이 있을 때만 호출된다.
 */
public interface AnomalyAnalyzer {

    MonitorAnalysis analyze(MonitorSnapshot snapshot, AnomalyVerdict verdict);
}
