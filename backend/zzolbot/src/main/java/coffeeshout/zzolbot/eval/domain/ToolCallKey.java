package coffeeshout.zzolbot.eval.domain;

import java.util.Map;
import java.util.TreeMap;

/**
 * 도구 호출을 스냅샷에서 식별하기 위한 키.
 * 인자(Map)는 키 정렬로 정규화해 호출 순서·맵 구현에 무관하게 동일 호출이 동일 키가 되게 한다.
 */
public record ToolCallKey(String toolName, String canonicalArgs) {

    public static ToolCallKey of(String toolName, Map<String, Object> args) {
        final String canonical = (args == null || args.isEmpty())
                ? "{}"
                : new TreeMap<>(args).toString();
        return new ToolCallKey(toolName, canonical);
    }
}
