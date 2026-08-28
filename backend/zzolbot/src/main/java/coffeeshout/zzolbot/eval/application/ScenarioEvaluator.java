package coffeeshout.zzolbot.eval.application;

import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;

/**
 * 시나리오 1건을 replay해 봇 출력을 산출한다. kind별 구현이 박제 입력(snapshotJson)의
 * 해석과 봇 호출을 담당하고, 채점(judge)과 결과 영속은 {@link EvalRunService}가 공통으로 처리한다.
 * 새 검증 경로는 이 인터페이스의 구현을 추가하는 것으로 확장한다.
 */
public interface ScenarioEvaluator {

    ScenarioKind kind();

    EvalAnswer evaluate(EvalScenarioEntity scenario);
}
