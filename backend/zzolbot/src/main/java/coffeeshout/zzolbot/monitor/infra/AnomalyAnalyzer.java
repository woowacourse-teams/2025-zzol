package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import java.util.List;

/**
 * firing 알림을 LLM으로 요약·근본원인 분석한다. 예산이 있고 로그 근거가 있을 때만 호출된다.
 * {@code logSamples}는 알림 시점의 실제 ERROR 로그 샘플로, 알림 메타데이터를 넘어 내용 기반 분석에 쓰인다.
 * {@code logEnvironment}는 그 샘플을 어느 환경에서 가져왔는지로, 알림 대상 환경과 어긋나면
 * 분석이 무관한 근거 위에 서게 되므로 LLM이 이를 인지하고 판정할 수 있도록 함께 넘긴다(#1594).
 */
public interface AnomalyAnalyzer {

    MonitorAnalysis analyze(FiringAlert alert, List<String> logSamples, String logEnvironment);
}
