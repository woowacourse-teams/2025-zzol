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
            softly.assertThat(source.getMissingCount()).isZero();
        });
    }

    @Test
    void since_같은_윈도우_인자는_무시하고_매칭한다() {
        final ToolSnapshot snapshot = new ToolSnapshot(Map.of(
                ToolCallKey.of("loki_logs", Map.of("since", "1h")), "{\"logCount\":312}"));
        final SnapshotToolResultSource source = new SnapshotToolResultSource(snapshot);

        // 봇이 since를 3h로 줘도(LLM이 매번 다르게 생성) 같은 골든 결과에 매칭돼야 한다.
        final List<ToolExecutionResult> results = source.executeAll(
                List.of(new ToolCallItem("loki_logs", Map.of("since", "3h"))), null);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(results.get(0).success()).isTrue();
            softly.assertThat(results.get(0).content()).isEqualTo("{\"logCount\":312}");
            softly.assertThat(source.getMissingCount()).isZero();
        });
    }

    @Test
    void joinCode_같은_식별_인자는_구분한다() {
        final ToolSnapshot snapshot = new ToolSnapshot(Map.of(
                ToolCallKey.of("loki_logs", Map.of("joinCode", "A4BX")), "{\"room\":\"A4BX\"}"));
        final SnapshotToolResultSource source = new SnapshotToolResultSource(snapshot);

        final List<ToolExecutionResult> results = source.executeAll(
                List.of(new ToolCallItem("loki_logs", Map.of("joinCode", "B7CD"))), null);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(results.get(0).success()).isFalse();   // 다른 방 → 매칭 안 됨
            softly.assertThat(source.getMissingCount()).isEqualTo(1);
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
            softly.assertThat(source.getMissingCount()).isEqualTo(1);
        });
    }
}
