package coffeeshout.zzolbot.infra;

import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ZzolBotSchemaConverter {

    @SuppressWarnings("unchecked")
    public Schema convert(Map<String, Object> jsonSchema) {
        final Schema.Builder builder = Schema.builder();

        final Object typeValue = jsonSchema.get("type");
        if (typeValue instanceof String typeStr) {
            builder.type(typeStr.toUpperCase());
        }

        final Object description = jsonSchema.get("description");
        if (description instanceof String descStr) {
            builder.description(descStr);
        }

        final Object properties = jsonSchema.get("properties");
        if (properties instanceof Map<?, ?> propsMap && !propsMap.isEmpty()) {
            final Map<String, Schema> converted = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : propsMap.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> propSchema) {
                    converted.put(
                            (String) entry.getKey(),
                            convert((Map<String, Object>) propSchema)
                    );
                }
            }
            builder.properties(converted);
        }

        final Object required = jsonSchema.get("required");
        if (required instanceof List<?> requiredList) {
            builder.required(requiredList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList());
        }

        return builder.build();
    }
}
