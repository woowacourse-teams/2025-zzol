package coffeeshout.contract;

import coffeeshout.websocket.docs.WsCatalog;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WebSocket payload OpenAPI 생성기")
class WsOpenApiEmitterTest {

    enum Kind {
        A,
        B
    }

    record Foo(
            long id,
            List<String> tags,
            @Nullable String note,
            @Nullable Kind kind) {}

    private static final WsCatalog CATALOG = new WsCatalog(
            "/ws",
            "/app",
            "/topic",
            "/queue",
            null,
            List.of(),
            List.of(),
            List.of(),
            Map.of(
                    "Foo",
                    new WsCatalog.SchemaEntry(
                            WsCatalog.SchemaKind.RECORD,
                            List.of(
                                    new WsCatalog.FieldEntry("id", "long"),
                                    new WsCatalog.FieldEntry("tags", "List<String>"),
                                    new WsCatalog.FieldEntry("note", "String?"),
                                    new WsCatalog.FieldEntry("kind", "Kind?")),
                            null),
                    "Kind",
                    new WsCatalog.SchemaEntry(WsCatalog.SchemaKind.ENUM, null, List.of("A", "B"))),
            new WsCatalog.ErrorShape("/queue/errors", "WebSocketResponse<String>"));

    @Test
    @DisplayName("@Nullable 이 아닌 필드만 required 이고, @Nullable 은 nullable 이며 $ref 는 allOf 로 감싼다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void nullable_표시를_required_와_nullable_로_옮긴다() {
        final OpenAPI doc = WsOpenApiEmitter.emit(CATALOG, Map.of("Foo", Foo.class, "Kind", Kind.class));
        final Schema foo = doc.getComponents().getSchemas().get("Foo");
        final Map<String, Schema> props = foo.getProperties();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(foo.getRequired()).containsExactly("id", "tags");
            softly.assertThat(props.get("note").getNullable()).isTrue();
            softly.assertThat(props.get("kind").getNullable()).isTrue();
            softly.assertThat(props.get("kind").getAllOf())
                    .extracting(item -> ((Schema) item).get$ref())
                    .containsExactly("#/components/schemas/Kind");
            softly.assertThat(props.get("id").getNullable()).isNull();
            softly.assertThat(doc.getComponents().getSchemas().get("Kind").getEnum())
                    .containsExactly("A", "B");
        });
    }
}
