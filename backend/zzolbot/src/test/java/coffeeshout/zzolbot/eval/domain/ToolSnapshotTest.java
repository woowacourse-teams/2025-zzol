package coffeeshout.zzolbot.eval.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToolSnapshotTest {

    @Nested
    class ToolCallKey_정규화 {

        @Test
        void 인자_맵의_순서가_달라도_같은_키가_된다() {
            final Map<String, Object> ordered = new LinkedHashMap<>();
            ordered.put("a", "1");
            ordered.put("b", "2");
            final Map<String, Object> reversed = new LinkedHashMap<>();
            reversed.put("b", "2");
            reversed.put("a", "1");

            final ToolCallKey key1 = ToolCallKey.of("room_state", ordered);
            final ToolCallKey key2 = ToolCallKey.of("room_state", reversed);

            assertThat(key1).isEqualTo(key2);
        }

        @Test
        void 인자가_없으면_빈_객체로_정규화된다() {
            final ToolCallKey key = ToolCallKey.of("redis_stream_status", Map.of());

            assertThat(key.canonicalArgs()).isEqualTo("{}");
        }

        @Test
        void 도구명이_다르면_다른_키가_된다() {
            final ToolCallKey roomState = ToolCallKey.of("room_state", Map.of("joinCode", "A4BX"));
            final ToolCallKey outbox = ToolCallKey.of("outbox_events", Map.of("joinCode", "A4BX"));

            assertThat(roomState).isNotEqualTo(outbox);
        }
    }

    @Nested
    class find_메서드 {

        @Test
        void 스냅샷에_있는_도구_호출은_결과를_반환한다() {
            final ToolSnapshot snapshot = new ToolSnapshot(Map.of(
                    ToolCallKey.of("room_state", Map.of("joinCode", "A4BX")), "{\"state\":\"PLAYING\"}"));

            assertThat(snapshot.find("room_state", Map.of("joinCode", "A4BX")))
                    .contains("{\"state\":\"PLAYING\"}");
        }

        @Test
        void 스냅샷에_없는_도구_호출은_빈_값을_반환한다() {
            final ToolSnapshot snapshot = new ToolSnapshot(Map.of(
                    ToolCallKey.of("room_state", Map.of("joinCode", "A4BX")), "{\"state\":\"PLAYING\"}"));

            assertThat(snapshot.find("room_state", Map.of("joinCode", "ZZZZ"))).isEmpty();
        }
    }
}
