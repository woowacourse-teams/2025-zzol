package coffeeshout.zzolbot.eval.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.eval.domain.ToolCallKey;
import coffeeshout.zzolbot.eval.domain.ToolSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolSnapshotCodecTest {

    private final ToolSnapshotCodec codec = new ToolSnapshotCodec(new ObjectMapper());

    @Test
    void 스냅샷을_JSON으로_직렬화한_뒤_복원하면_동일하게_조회된다() {
        final ToolSnapshot original = new ToolSnapshot(Map.of(
                ToolCallKey.of("room_state", Map.of("joinCode", "A4BX")), "{\"state\":\"PLAYING\"}",
                ToolCallKey.of("redis_stream_status", Map.of()), "{\"game-stream\":48213}"));

        final ToolSnapshot restored = codec.fromJson(codec.toJson(original));

        assertThat(restored.find("room_state", Map.of("joinCode", "A4BX")))
                .contains("{\"state\":\"PLAYING\"}");
        assertThat(restored.find("redis_stream_status", Map.of()))
                .contains("{\"game-stream\":48213}");
    }
}
