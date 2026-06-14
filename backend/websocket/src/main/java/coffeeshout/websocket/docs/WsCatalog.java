package coffeeshout.websocket.docs;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import java.util.Map;

public record WsCatalog(
        String stompEndpoint,
        String app,
        String topicPrefix,
        String queuePrefix,
        Envelope envelope,
        List<TopicEntry> topics,
        List<QueueEntry> queues,
        List<SendEntry> sends,
        Map<String, SchemaEntry> schemas,
        ErrorShape errors
) {

    public enum SchemaKind {
        RECORD, ENUM, OBJECT;

        @JsonValue
        public String wire() {
            return name().toLowerCase();
        }
    }

    public record Envelope(String type, List<FieldEntry> fields, String note) {
    }

    public record TopicEntry(String path, String payloadType, List<Publisher> publishers, List<String> referencedSchemas) {
    }

    public record QueueEntry(String path, String payloadType, List<Publisher> publishers, List<String> referencedSchemas) {
    }

    public record SendEntry(String destination, String description, String requestType, List<String> triggersTopics, Source source, List<String> referencedSchemas) {
    }

    public record Publisher(String description, Source source) {
    }

    public record Source(String className, String methodName) {
    }

    public record SchemaEntry(SchemaKind kind, List<FieldEntry> fields, List<String> values) {
    }

    public record FieldEntry(String name, String type) {
    }

    public record ErrorShape(String topic, String payloadType) {
    }
}
