package coffeeshout.zzolbot.eval.domain;

import java.util.Map;
import java.util.Optional;

/**
 * 시나리오별로 박제된 도구 결과 모음. replay 시 라이브 도구 대신 이 스냅샷이 결과를 공급한다.
 * 스냅샷에 없는 도구 호출은 {@link Optional#empty()}로 신호해 시나리오 커버리지 갭을 드러낸다.
 */
public record ToolSnapshot(Map<ToolCallKey, String> results) {

    public ToolSnapshot {
        results = Map.copyOf(results);
    }

    public Optional<String> find(String toolName, Map<String, Object> args) {
        return Optional.ofNullable(results.get(ToolCallKey.of(toolName, args)));
    }
}
