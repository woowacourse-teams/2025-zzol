package coffeeshout.zzolbot.eval.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem;
import coffeeshout.zzolbot.eval.domain.ToolCallKey;
import coffeeshout.zzolbot.eval.domain.ToolSnapshot;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class SnapshotToolResultSourceTest {

    @Test
    void 스냅샷에_있는_호출은_성공_결과로_반환한다() {
        final ToolSnapshot snapshot = new ToolSnapshot(Map.of(
                ToolCallKey.of("room_state", Map.of("joinCode", "A4BX")), "{\"state\":\"PLAYING\"}"));
        final SnapshotToolResultSource source = new SnapshotToolResultSource(snapshot);

        final List<ToolExecutionResult> results = source.executeAll(
                List.of(new ToolCallItem("room_state", Map.of("joinCode", "A4BX"))), null);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(results).hasSize(1);
            softly.assertThat(results.get(0).success()).isTrue();
            softly.assertThat(results.get(0).content()).isEqualTo("{\"state\":\"PLAYING\"}");
            softly.assertThat(source.missingCount()).isZero();
        });
    }

    @Test
    void 스냅샷에_없는_호출은_실패로_처리하고_missingCount를_증가시킨다() {
        final ToolSnapshot snapshot = new ToolSnapshot(Map.of(
                ToolCallKey.of("room_state", Map.of("joinCode", "A4BX")), "{\"state\":\"PLAYING\"}"));
        final SnapshotToolResultSource source = new SnapshotToolResultSource(snapshot);

        final List<ToolExecutionResult> results = source.executeAll(
                List.of(new ToolCallItem("loki_logs", Map.of("joinCode", "A4BX"))), null);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(results.get(0).success()).isFalse();
            softly.assertThat(source.missingCount()).isEqualTo(1);
        });
    }
}
