package coffeeshout.global.websocket.docs;

import java.util.List;
import java.util.Map;

public record WsCatalog(
        String stompEndpoint,
        String app,
        String topicPrefix,
        String queuePrefix,
        Info info,
        Envelope envelope,
        List<TopicEntry> topics,
        List<QueueEntry> queues,
        List<SendEntry> sends,
        Map<String, SchemaEntry> schemas,
        ErrorShape errors
) {

    public record Info(String title, String version, String description) {
    }

    public record Envelope(String type, List<FieldEntry> fields, String note) {
    }

    public record TopicEntry(String path, String description, String payloadType, Source source) {
    }

    public record QueueEntry(String path, String description, String payloadType, Source source) {
    }

    public record SendEntry(String destination, String description, String requestType, List<String> triggersTopics, Source source) {
    }

    public record Source(String className, String methodName) {
    }

    public record SchemaEntry(String kind, List<FieldEntry> fields, List<String> values) {
    }

    public record FieldEntry(String name, String type) {
    }

    public record ErrorShape(String topic, String payloadType) {
    }
}
