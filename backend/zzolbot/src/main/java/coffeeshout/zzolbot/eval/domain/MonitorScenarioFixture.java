package coffeeshout.zzolbot.eval.domain;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import java.util.List;

/**
 * 알림 분석(MONITOR) 시나리오의 박제 입력. 분석 포트({@code AnomalyAnalyzer})의 입력 3요소를
 * 그대로 고정한다 — 챗봇 시나리오가 도구 결과를 박제하듯, 여기서는 알림과 로그 샘플을 박제해
 * 라이브 데이터 변동 없이 LLM 추론만 비교 가능하게 한다.
 */
public record MonitorScenarioFixture(
        FiringAlert alert,
        List<String> logSamples,
        String logEnvironment
) {

    public MonitorScenarioFixture {
        logSamples = List.copyOf(logSamples);
    }
}
