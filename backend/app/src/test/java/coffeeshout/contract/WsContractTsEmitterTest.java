package coffeeshout.contract;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.websocket.docs.WsCatalog;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WebSocket 계약 TS 생성기")
class WsContractTsEmitterTest {

    private static final WsCatalog CATALOG = new WsCatalog(
            "/ws",
            "/app",
            "/topic",
            "/queue",
            null,
            List.of(
                    new WsCatalog.TopicEntry(
                            "/topic/room/{joinCode:.{4}}/x", "WebSocketResponse<List<Foo>>", List.of(), List.of()),
                    new WsCatalog.TopicEntry("/topic/room/{joinCode}", "WebSocketResponse<Foo>", List.of(), List.of()),
                    new WsCatalog.TopicEntry("/topic/room/{a}/{b}", "WebSocketResponse<Foo>", List.of(), List.of()),
                    new WsCatalog.TopicEntry("/topic/room/{a}/y", "WebSocketResponse<Kind>", List.of(), List.of())),
            List.of(new WsCatalog.QueueEntry(
                    "/user/queue/friends/presence", "WebSocketResponse<T>", List.of(), List.of())),
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
                                    new WsCatalog.FieldEntry("poles", "List<Foo>?"),
                                    new WsCatalog.FieldEntry("raw", "JsonNode"),
                                    new WsCatalog.FieldEntry("kind", "Kind")),
                            null),
                    "Kind",
                    new WsCatalog.SchemaEntry(WsCatalog.SchemaKind.ENUM, null, List.of("A", "B"))),
            new WsCatalog.ErrorShape("/queue/errors", "WebSocketResponse<String>"));

    private final String ts = WsContractTsEmitter.emit(CATALOG);

    @Nested
    @DisplayName("destination union")
    class Destination_union {

        @Test
        @DisplayName("prefix 를 떼고 경로 변수를 ${string} 으로 바꾸며 빈 union 은 never 로 낸다")
        void 경로를_TS_리터럴로_옮긴다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(ts).contains("  | `/room/${string}/x`\n");
                softly.assertThat(ts).contains("  | '/user/queue/friends/presence';");
                softly.assertThat(ts).contains("  | '/user/queue/errors';");
                softly.assertThat(ts).contains("export type WsSendPath = never;");
            });
        }
    }

    @Nested
    @DisplayName("payload 타입")
    class Payload_타입 {

        @Test
        @DisplayName("record 는 객체, enum 은 리터럴 union, ? 접미사는 optional | null 이 된다")
        void 스키마를_TS_타입으로_옮긴다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(ts)
                        .contains("export type Foo = {\n"
                                + "  id: number;\n"
                                + "  tags: string[];\n"
                                + "  ranks: Record<string, number>;\n"
                                + "  note?: string | null;\n"
                                + "  poles?: Foo[] | null;\n"
                                + "  raw: unknown;\n"
                                + "  kind: Kind;\n"
                                + "};");
                softly.assertThat(ts).contains("export type Kind = 'A' | 'B';");
            });
        }
    }

    @Nested
    @DisplayName("WsPayloadOf 체인")
    class WsPayloadOf_체인 {

        @Test
        @DisplayName("세그먼트가 많은 패턴, 같으면 리터럴이 많은 패턴이 앞에 온다")
        void 넓은_패턴이_좁은_경로를_가로채지_않는다() {
            final int specific = ts.indexOf("D extends `/room/${string}/x` ? Foo[] :");
            final int literalTail = ts.indexOf("D extends `/room/${string}/y` ? Kind :");
            final int doubleWildcard = ts.indexOf("D extends `/room/${string}/${string}` ? Foo :");
            final int root = ts.indexOf("D extends `/room/${string}` ? Foo :");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(specific).isGreaterThan(-1).isLessThan(root);
                softly.assertThat(literalTail).isGreaterThan(-1).isLessThan(doubleWildcard);
                softly.assertThat(doubleWildcard).isLessThan(root);
                softly.assertThat(ts).contains("D extends '/user/queue/friends/presence' ? unknown :");
                softly.assertThat(ts).contains("D extends '/user/queue/errors' ? string :");
            });
        }
    }

    @Test
    @DisplayName("생성 결과는 never 로 끝난다")
    void 체인은_never_로_끝난다() {
        assertThat(ts).endsWith("  never;\n");
    }
}
