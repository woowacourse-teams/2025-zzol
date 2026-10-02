package coffeeshout.contract;

import coffeeshout.websocket.docs.WsCatalog;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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

    interface Cmd {}

    record Start(String hostName) implements Cmd {}

    record Select(int cardIndex) implements Cmd {}

    record Envelope(
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
            @JsonSubTypes({
                @JsonSubTypes.Type(value = Start.class, name = "START"),
                @JsonSubTypes.Type(value = Select.class, name = "SELECT")
            })
            Cmd body) {}

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

    private static final WsCatalog POLY_CATALOG = new WsCatalog(
            "/ws",
            "/app",
            "/topic",
            "/queue",
            null,
            List.of(),
            List.of(),
            List.of(),
            Map.of(
                    "Envelope",
                    new WsCatalog.SchemaEntry(
                            WsCatalog.SchemaKind.RECORD, List.of(new WsCatalog.FieldEntry("body", "Cmd")), null)),
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

    @Test
    @DisplayName("EXTERNAL_PROPERTY 필드는 하위 타입마다 판별 값이 고정된 oneOf 변형이 된다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void 외부_타입_id_필드를_oneOf_로_가른다() {
        final OpenAPI doc = WsOpenApiEmitter.emit(
                POLY_CATALOG, Map.of("Envelope", Envelope.class, "Start", Start.class, "Select", Select.class));
        final Map<String, Schema> schemas = doc.getComponents().getSchemas();
        final List<Schema> variants = schemas.get("Envelope").getOneOf();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(variants)
                    .extracting(v -> ((Schema) v.getProperties().get("type")).getEnum())
                    .containsExactly(List.of("START"), List.of("SELECT"));
            softly.assertThat(variants)
                    .extracting(v -> ((Schema) v.getProperties().get("body")).get$ref())
                    .containsExactly("#/components/schemas/Start", "#/components/schemas/Select");
            softly.assertThat(variants)
                    .allSatisfy(v -> softly.assertThat(v.getRequired()).containsExactlyInAnyOrder("body", "type"));
            softly.assertThat(schemas).doesNotContainKey("Cmd");
        });
    }
}
