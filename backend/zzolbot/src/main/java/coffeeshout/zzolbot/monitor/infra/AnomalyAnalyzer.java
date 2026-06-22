package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import java.util.List;

/**
 * firing 알림을 LLM으로 요약·근본원인 분석한다. 예산이 있을 때만 호출된다.
 * {@code logSamples}는 알림 시점의 실제 ERROR 로그 샘플로, 알림 메타데이터를 넘어 내용 기반 분석에 쓰인다.
 */
public interface AnomalyAnalyzer {

    MonitorAnalysis analyze(FiringAlert alert, List<String> logSamples);
}
