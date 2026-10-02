package coffeeshout.contract;

import coffeeshout.websocket.docs.WsCatalog;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        schemaClasses.forEach((name, cls) -> {
            if (cls.isRecord()) {
                splitByExternalTypeId(schemas, name, cls);
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

    /**
     * {@code @JsonTypeInfo(include = EXTERNAL_PROPERTY)} 필드는 형제 프로퍼티 값이 그 필드의 구체 타입을 정한다.
     * swagger-core 는 이 짝을 모르고 인터페이스를 빈 object 로 낸다. 하위 타입마다 변형 하나를 만들어 {@code oneOf}
     * 로 묶으면 openapi-typescript 가 {@code {commandType: 'A'; commandRequest: A} | ...} 처럼 판별 가능한 union 을 낸다.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void splitByExternalTypeId(Map<String, Schema> schemas, String name, Class<?> cls) {
        for (final RecordComponent component : cls.getRecordComponents()) {
            final JsonTypeInfo typeInfo = component.getAccessor().getAnnotation(JsonTypeInfo.class);
            final JsonSubTypes subTypes = component.getAccessor().getAnnotation(JsonSubTypes.class);
            if (typeInfo == null || subTypes == null || typeInfo.include() != JsonTypeInfo.As.EXTERNAL_PROPERTY) {
                continue;
            }
            final Schema record = schemas.get(name);
            final ComposedSchema union = new ComposedSchema();
            for (final JsonSubTypes.Type subType : subTypes.value()) {
                final Schema variant = new Schema().type("object");
                variant.setProperties(new LinkedHashMap<String, Schema>(record.getProperties()));
                variant.addProperty(typeInfo.property(), new StringSchema()._enum(List.of(subType.name())));
                variant.addProperty(
                        component.getName(),
                        new Schema()
                                .$ref("#/components/schemas/" + subType.value().getSimpleName()));
                final List<String> required = new ArrayList<>(record.getRequired());
                required.add(typeInfo.property());
                variant.setRequired(required);
                union.addOneOfItem(variant);
            }
            schemas.put(name, union);
            // 변형마다 하위 타입을 직접 가리키므로 swagger-core 가 낸 빈 인터페이스 스키마는 아무도 안 쓴다.
            schemas.remove(component.getType().getSimpleName());
        }
    }
}
