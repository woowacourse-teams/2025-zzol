package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import java.util.List;

/**
 * 이상 신호를 LLM으로 요약·근본원인 분석한다. 이상으로 판정되고 예산이 있을 때만 호출된다.
 * {@code logSamples}는 이상 시점의 실제 ERROR/WARN 로그 샘플로, 단순 수치를 넘어 내용 기반 분석에 쓰인다.
 */
public interface AnomalyAnalyzer {

    MonitorAnalysis analyze(MonitorSnapshot snapshot, AnomalyVerdict verdict, List<String> logSamples);
}
