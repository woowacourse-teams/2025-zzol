package coffeeshout.contract;

import coffeeshout.websocket.docs.WsCatalog;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WebSocket 계약 TS 생성기")
class WsContractTsEmitterTest {

    @Test
    @DisplayName("경로·payload·nullable 을 TS 로 옮기고 빈 union 은 never 로 낸다")
    void 경로와_payload_와_nullable_을_TS_로_옮긴다() {
        final WsCatalog catalog = new WsCatalog(
                "/ws",
                "/app",
                "/topic",
                "/queue",
                null,
                List.of(
                        new WsCatalog.TopicEntry(
                                "/topic/room/{joinCode:.{4}}/x", "WebSocketResponse<List<Foo>>", List.of(), List.of()),
                        new WsCatalog.TopicEntry(
                                "/topic/room/{joinCode}", "WebSocketResponse<Foo>", List.of(), List.of())),
                List.of(new WsCatalog.QueueEntry("/user/queue/friends/presence", "T", List.of(), List.of())),
                List.of(),
                Map.of(
                        "Foo",
                        new WsCatalog.SchemaEntry(
                                WsCatalog.SchemaKind.RECORD,
                                List.of(
                                        new WsCatalog.FieldEntry("id", "Long"),
                                        new WsCatalog.FieldEntry("tags", "List<String>"),
                                        new WsCatalog.FieldEntry("ranks", "Map<Integer, Integer>"),
                                        new WsCatalog.FieldEntry("note", "String?"),
                                        new WsCatalog.FieldEntry("kind", "Kind")),
                                null),
                        "Kind",
                        new WsCatalog.SchemaEntry(WsCatalog.SchemaKind.ENUM, null, List.of("A", "B"))),
                new WsCatalog.ErrorShape("/queue/errors", "WebSocketResponse<String>"));

        final String ts = WsContractTsEmitter.emit(catalog);

        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(ts).contains("  | `/room/${string}/x`\n");
        softly.assertThat(ts).contains("  | '/user/queue/friends/presence';");
        softly.assertThat(ts).contains("  | '/user/queue/errors';");
        softly.assertThat(ts).contains("export type WsSendPath = never;");
        softly.assertThat(ts)
                .contains("export type Foo = {\n  id: number;\n  tags: string[];\n  ranks: Record<string, number>;\n"
                        + "  note?: string | null;\n  kind: Kind;\n};");
        softly.assertThat(ts).contains("export type Kind = 'A' | 'B';");
        // 세그먼트가 많은 `/room/${string}/x` 가 `/room/${string}` 보다 앞에 와야 가로채이지 않는다
        softly.assertThat(ts.indexOf("D extends `/room/${string}/x` ? Foo[] :"))
                .isLessThan(ts.indexOf("D extends `/room/${string}` ? Foo :"));
        softly.assertThat(ts).contains("D extends '/user/queue/errors' ? string :");
        softly.assertAll();
    }
}
