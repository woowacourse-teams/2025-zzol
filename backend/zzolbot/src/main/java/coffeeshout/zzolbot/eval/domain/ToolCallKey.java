package coffeeshout.zzolbot.eval.domain;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 도구 호출을 스냅샷에서 식별하기 위한 키.
 * 인자(Map)는 키 정렬로 정규화해 호출 순서·맵 구현에 무관하게 동일 호출이 동일 키가 되게 한다.
 *
 * <p>{@code since}·{@code limit} 같은 윈도우/페이징 인자는 "어떤 데이터를 보는지"를 바꾸지 않는
 * 부수적 인자라 키에서 제외한다. 그래야 봇이 {@code since}를 1h로 주든 3h로 주든 같은 골든 스냅샷에
 * 매칭된다. {@code joinCode}·{@code query} 같은 식별 인자는 유지한다.
 */
public record ToolCallKey(String toolName, String canonicalArgs) {

    private static final Set<String> INCIDENTAL_ARGS = Set.of("since", "limit", "start", "end", "direction", "time");

    public static ToolCallKey of(String toolName, Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return new ToolCallKey(toolName, "{}");
        }
        final TreeMap<String, Object> identifying = new TreeMap<>(args);
        identifying.keySet().removeAll(INCIDENTAL_ARGS);
        return new ToolCallKey(toolName, identifying.isEmpty() ? "{}" : identifying.toString());
    }
}
