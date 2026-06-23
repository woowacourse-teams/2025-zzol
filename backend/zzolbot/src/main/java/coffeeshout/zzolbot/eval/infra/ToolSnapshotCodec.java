package coffeeshout.zzolbot.eval.infra;

import coffeeshout.zzolbot.eval.domain.ToolCallKey;
import coffeeshout.zzolbot.eval.domain.ToolSnapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link ToolSnapshot}을 DB 저장용 JSON 문자열로 직렬화/역직렬화한다.
 */
@Component
@RequiredArgsConstructor
public class ToolSnapshotCodec {

    private final ObjectMapper objectMapper;

    public String toJson(ToolSnapshot snapshot) {
        final List<Entry> entries = snapshot.results().entrySet().stream()
                .map(e -> new Entry(e.getKey().toolName(), e.getKey().canonicalArgs(), e.getValue()))
                .toList();
        try {
            return objectMapper.writeValueAsString(entries);
        } catch (Exception e) {
            throw new IllegalStateException("도구 스냅샷 직렬화 실패", e);
        }
    }

    public ToolSnapshot fromJson(String json) {
        try {
            final List<Entry> entries = objectMapper.readValue(json, new TypeReference<>() {
            });
            final Map<ToolCallKey, String> results = new LinkedHashMap<>();
            for (Entry entry : entries) {
                results.put(new ToolCallKey(entry.toolName(), entry.canonicalArgs()), entry.content());
            }
            return new ToolSnapshot(results);
        } catch (Exception e) {
            throw new IllegalStateException("도구 스냅샷 역직렬화 실패", e);
        }
    }

    private record Entry(String toolName, String canonicalArgs, String content) {
    }
}
