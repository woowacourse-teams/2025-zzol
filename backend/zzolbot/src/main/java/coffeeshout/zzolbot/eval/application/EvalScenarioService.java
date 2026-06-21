package coffeeshout.zzolbot.eval.application;

import coffeeshout.zzolbot.eval.domain.ScenarioSource;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.EvalScenarioRepository;
import coffeeshout.zzolbot.eval.infra.ToolSnapshotCodec;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 골든 시나리오 등록·조회. 녹화 기반(RECORDED)과 수기(MANUAL) 등록을 제공한다.
 * 녹화는 라이브 Gemini 호출을 포함하므로 트랜잭션 밖에서 수행하고 저장만 영속화한다.
 */
@Service
@RequiredArgsConstructor
public class EvalScenarioService {

    private final EvalScenarioRepository scenarioRepository;
    private final ScenarioRecorder recorder;
    private final ToolSnapshotCodec codec;

    @Transactional(readOnly = true)
    public List<EvalScenarioEntity> list() {
        return scenarioRepository.findAllByOrderByCreatedAtDesc();
    }

    public EvalScenarioEntity registerRecorded(String name, String question, String rubric, String adminUsername) {
        final ScenarioRecorder.Recorded recorded = recorder.record(question, adminUsername);
        final String snapshotJson = codec.toJson(recorded.snapshot());
        return scenarioRepository.save(
                EvalScenarioEntity.create(name, question, snapshotJson, rubric, ScenarioSource.RECORDED));
    }

    public EvalScenarioEntity registerManual(String name, String question, String snapshotJson, String rubric) {
        codec.fromJson(snapshotJson); // 잘못된 스냅샷 JSON을 저장 전에 빠르게 검증(실패 시 예외) — 이후 실행 깨짐 방지
        return scenarioRepository.save(
                EvalScenarioEntity.create(name, question, snapshotJson, rubric, ScenarioSource.MANUAL));
    }

    @Transactional
    public void delete(Long id) {
        scenarioRepository.deleteById(id);
    }
}
