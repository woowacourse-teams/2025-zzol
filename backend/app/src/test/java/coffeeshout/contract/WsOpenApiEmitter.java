package coffeeshout.contract;

import coffeeshout.websocket.docs.WsCatalog;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.JsonNode;

/**
 * 카탈로그가 참조하는 payload record 를 OpenAPI {@code components.schemas} 로 낸다. TS 타입은 FE 의
 * {@code openapi-typescript} 가 이 문서에서 만들므로 여기서는 Java 타입을 TS 로 옮기지 않는다.
 *
 * <p>스키마 자체는 springdoc 이 REST 문서에 쓰는 swagger-core {@link ModelConverters} 가 만든다. 그 결과에는
 * {@code required} 가 없어 모든 필드가 선택이 되므로, 카탈로그의 {@code @Nullable} 표시({@code ?} 접미사)로
 * {@code required} 와 {@code nullable} 을 채운다. FE 에서는 {@code @Nullable} 필드만 {@code field?: T | null} 이 된다.
 */
final class WsOpenApiEmitter {

    static {
        // 기본값은 enum 을 필드마다 인라인으로 푼다. FE 가 enum 이름(PlayerType 등)을 import 하므로 이름 있는 스키마로 낸다.
        ModelResolver.enumsAsRef = true;
        // JsonNode 는 빈으로 풀면 가짜 스키마가 된다. 빈 스키마로 두면 openapi-typescript 가 unknown 으로 낸다.
        ModelConverters.getInstance().addConverter((type, context, chain) -> {
            if (JsonNode.class.isAssignableFrom(
                    Json.mapper().constructType(type.getType()).getRawClass())) {
                return new Schema<>();
            }
            return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        });
    }

    private WsOpenApiEmitter() {}

    @SuppressWarnings("rawtypes")
    static OpenAPI emit(WsCatalog catalog, Map<String, Class<?>> schemaClasses) {
        final Map<String, Schema> schemas = new TreeMap<>();
        schemaClasses
                .values()
                .forEach(cls -> schemas.putAll(ModelConverters.getInstance().readAll(cls)));
        catalog.schemas().forEach((name, entry) -> {
            if (entry.kind() == WsCatalog.SchemaKind.RECORD && schemas.containsKey(name)) {
                applyNullability(schemas.get(name), entry);
            }
        });
        return new OpenAPI()
                .info(new Info().title("쫄 WebSocket payload").version("1"))
                .paths(new Paths())
                .components(new Components().schemas(schemas));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyNullability(Schema record, WsCatalog.SchemaEntry entry) {
        final List<String> required = new ArrayList<>();
        for (final WsCatalog.FieldEntry field : entry.fields()) {
            if (!field.type().endsWith("?")) {
                required.add(field.name());
                continue;
            }
            final Schema property = (Schema) record.getProperties().get(field.name());
            // `$ref` 옆의 nullable 은 OAS 3.0 에서 무시된다. allOf 로 감싸야 openapi-typescript 가 `| null` 을 붙인다.
            if (property.get$ref() != null) {
                final ComposedSchema wrapped = new ComposedSchema();
                wrapped.addAllOfItem(new Schema().$ref(property.get$ref()));
                wrapped.setNullable(true);
                record.getProperties().put(field.name(), wrapped);
            } else {
                property.setNullable(true);
            }
        }
        record.setRequired(required);
    }
}
