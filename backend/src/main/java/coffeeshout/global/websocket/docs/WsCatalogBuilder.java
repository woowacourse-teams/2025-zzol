package coffeeshout.global.websocket.docs;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prod")
public class WsCatalogBuilder {

    private final ApplicationContext applicationContext;
    private final WsCatalogProperties properties;

    public WsCatalogBuilder(ApplicationContext applicationContext, WsCatalogProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    public WsCatalog build() {
        final List<WsCatalog.TopicEntry> topics = new ArrayList<>();
        final List<WsCatalog.QueueEntry> queues = new ArrayList<>();
        final List<WsCatalog.SendEntry> sends = new ArrayList<>();
        final Set<Class<?>> referenced = new HashSet<>();

        final Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        for (final Object bean : beans.values()) {
            collectFromBean(bean, topics, queues, sends, referenced);
        }

        topics.sort(Comparator.comparing(WsCatalog.TopicEntry::path));
        queues.sort(Comparator.comparing(WsCatalog.QueueEntry::path));
        sends.sort(Comparator.comparing(WsCatalog.SendEntry::destination));

        final List<WsCatalog.TopicEntry> deduped = deduplicateTopics(topics);
        final List<WsCatalog.QueueEntry> dedupedQueues = deduplicateQueues(queues);

        final Map<String, WsCatalog.SchemaEntry> schemas = expandSchemas(referenced);

        return new WsCatalog(
                properties.stompEndpoint(),
                properties.appPath(),
                properties.topicPath(),
                properties.queuePath(),
                toInfo(properties.info()),
                buildEnvelope(),
                deduped,
                dedupedQueues,
                sends,
                schemas,
                new WsCatalog.ErrorShape(properties.errorTopic(), properties.envelopeType() + "<String>")
        );
    }

    private void collectFromBean(
            Object bean,
            List<WsCatalog.TopicEntry> topics,
            List<WsCatalog.QueueEntry> queues,
            List<WsCatalog.SendEntry> sends,
            Set<Class<?>> referenced
    ) {
        final Class<?> targetClass = AopUtils.getTargetClass(bean);
        for (final Method method : targetClass.getDeclaredMethods()) {
            final WsTopic[] wsTopics = method.getAnnotationsByType(WsTopic.class);
            final WsQueue[] wsQueues = method.getAnnotationsByType(WsQueue.class);
            final MessageMapping messageMapping = AnnotationUtils.findAnnotation(method, MessageMapping.class);
            final WsReceive wsReceive = AnnotationUtils.findAnnotation(method, WsReceive.class);

            if (wsTopics.length == 0 && wsQueues.length == 0 && messageMapping == null && wsReceive == null) {
                continue;
            }
            final WsCatalog.Source source = new WsCatalog.Source(targetClass.getSimpleName(), method.getName());
            for (final WsTopic wsTopic : wsTopics) {
                topics.add(toTopicEntry(wsTopic, source, referenced));
            }
            for (final WsQueue wsQueue : wsQueues) {
                queues.add(toQueueEntry(wsQueue, source, referenced));
            }
            if (messageMapping != null) {
                sends.addAll(toSendEntries(method, messageMapping, wsTopics, wsReceive, source, referenced));
            }
        }
    }

    private List<WsCatalog.TopicEntry> deduplicateTopics(List<WsCatalog.TopicEntry> topics) {
        final Map<String, WsCatalog.TopicEntry> byPath = new LinkedHashMap<>();
        for (final WsCatalog.TopicEntry topic : topics) {
            final WsCatalog.TopicEntry existing = byPath.putIfAbsent(topic.path(), topic);
            if (existing != null && !existing.source().equals(topic.source())) {
                log.warn("동일 토픽 path 에 다중 publisher 선언: path={}, kept={}, dropped={}",
                        topic.path(), existing.source(), topic.source());
            }
        }
        return List.copyOf(byPath.values());
    }

    private List<WsCatalog.QueueEntry> deduplicateQueues(List<WsCatalog.QueueEntry> queues) {
        final Map<String, WsCatalog.QueueEntry> byPath = new LinkedHashMap<>();
        for (final WsCatalog.QueueEntry queue : queues) {
            byPath.putIfAbsent(queue.path(), queue);
        }
        return List.copyOf(byPath.values());
    }

    private List<String> collectTriggerTopics(WsTopic[] wsTopics, WsReceive wsReceive, WsCatalog.Source source) {
        final Stream<String> fromTopics = Arrays.stream(wsTopics)
                .map(t -> properties.topicPath() + t.path());
        final Stream<String> fromReceive = Optional.ofNullable(wsReceive)
                .stream()
                .flatMap(r -> Arrays.stream(r.respondsOnTopics()))
                .map(path -> validatedTopicPath(path, source));
        return Stream.concat(fromTopics, fromReceive).collect(Collectors.toList());
    }

    private String validatedTopicPath(String path, WsCatalog.Source source) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "@WsReceive.respondsOnTopics 에 빈 경로가 포함되어 있습니다: %s#%s".formatted(
                            source.className(), source.methodName()));
        }
        return properties.topicPath() + path;
    }

    private WsCatalog.QueueEntry toQueueEntry(WsQueue wsQueue, WsCatalog.Source source, Set<Class<?>> referenced) {
        if (wsQueue.path().isBlank()) {
            throw new IllegalArgumentException(
                    "@WsQueue.path 가 비어 있습니다: %s#%s".formatted(source.className(), source.methodName()));
        }
        if (!wsQueue.path().startsWith("/")) {
            throw new IllegalArgumentException(
                    "@WsQueue.path 는 '/' 로 시작해야 합니다: %s#%s path=%s".formatted(
                            source.className(), source.methodName(), wsQueue.path()));
        }
        if (wsQueue.payload() == Void.class) {
            throw new IllegalArgumentException(
                    "@WsQueue.payload 가 Void.class 입니다: %s#%s path=%s".formatted(
                            source.className(), source.methodName(), wsQueue.path()));
        }
        final String fullPath = properties.userDestinationPrefix() + wsQueue.path();
        final String payloadType = describePayloadType(wsQueue.payload(), wsQueue.generic(), referenced);
        return new WsCatalog.QueueEntry(fullPath, wsQueue.description(), payloadType, source);
    }

    private WsCatalog.TopicEntry toTopicEntry(WsTopic wsTopic, WsCatalog.Source source, Set<Class<?>> referenced) {
        if (wsTopic.path().isBlank()) {
            throw new IllegalArgumentException(
                    "@WsTopic.path 가 비어 있습니다: %s#%s".formatted(source.className(), source.methodName()));
        }
        if (!wsTopic.path().startsWith("/")) {
            throw new IllegalArgumentException(
                    "@WsTopic.path 는 '/' 로 시작해야 합니다: %s#%s path=%s".formatted(
                            source.className(), source.methodName(), wsTopic.path()));
        }
        if (wsTopic.payload() == Void.class) {
            throw new IllegalArgumentException(
                    "@WsTopic.payload 가 Void.class 입니다: %s#%s path=%s".formatted(
                            source.className(), source.methodName(), wsTopic.path()));
        }
        final String fullPath = properties.topicPath() + wsTopic.path();
        final String payloadType = describePayloadType(wsTopic.payload(), wsTopic.generic(), referenced);
        return new WsCatalog.TopicEntry(fullPath, wsTopic.description(), payloadType, source);
    }

    private List<WsCatalog.SendEntry> toSendEntries(
            Method method,
            MessageMapping mapping,
            WsTopic[] wsTopics,
            WsReceive wsReceive,
            WsCatalog.Source source,
            Set<Class<?>> referenced
    ) {
        final String requestType = findRequestPayload(method, referenced);
        final List<String> triggersTopics = collectTriggerTopics(wsTopics, wsReceive, source);
        final String description = resolveDescription(wsTopics, wsReceive);
        final String[] values = mapping.value();
        final String[] destinations = values.length == 0 ? new String[]{""} : values;
        final List<WsCatalog.SendEntry> entries = new ArrayList<>();
        for (final String mappingPath : destinations) {
            entries.add(new WsCatalog.SendEntry(properties.appPath() + mappingPath, description, requestType, triggersTopics, source));
        }
        return entries;
    }

    private String resolveDescription(WsTopic[] wsTopics, WsReceive wsReceive) {
        if (wsTopics.length > 0) {
            return wsTopics[0].description();
        }
        if (wsReceive != null) {
            return wsReceive.description();
        }
        return "";
    }

    private String describePayloadType(Class<?> payload, Class<?> generic, Set<Class<?>> referenced) {
        if (payload == Void.class) {
            return null;
        }
        if (generic == Void.class) {
            registerIfDescribable(payload, referenced);
            return properties.envelopeType() + "<" + payload.getSimpleName() + ">";
        }
        registerIfDescribable(payload, referenced);
        registerIfDescribable(generic, referenced);
        return properties.envelopeType() + "<" + payload.getSimpleName() + "<" + generic.getSimpleName() + ">>";
    }

    private String findRequestPayload(Method method, Set<Class<?>> referenced) {
        for (final Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(Payload.class)) {
                final Class<?> type = parameter.getType();
                registerIfDescribable(type, referenced);
                return type.getSimpleName();
            }
        }
        return null;
    }

    private Map<String, WsCatalog.SchemaEntry> expandSchemas(Set<Class<?>> seeds) {
        final Map<String, WsCatalog.SchemaEntry> result = new LinkedHashMap<>();
        final Deque<Class<?>> stack = new ArrayDeque<>(seeds);
        final Set<Class<?>> visited = new HashSet<>();
        while (!stack.isEmpty()) {
            final Class<?> cls = stack.pop();
            if (!visited.add(cls)) {
                continue;
            }
            result.put(cls.getSimpleName(), describeClass(cls, stack));
        }
        return result;
    }

    private WsCatalog.SchemaEntry describeClass(Class<?> cls, Deque<Class<?>> pending) {
        if (cls.isEnum()) {
            return describeEnum(cls);
        }
        if (cls.isRecord()) {
            return describeRecord(cls, pending);
        }
        return new WsCatalog.SchemaEntry("object", List.of(), null);
    }

    private WsCatalog.SchemaEntry describeEnum(Class<?> cls) {
        final List<String> values = new ArrayList<>();
        for (final Object constant : cls.getEnumConstants()) {
            values.add(((Enum<?>) constant).name());
        }
        return new WsCatalog.SchemaEntry("enum", null, values);
    }

    private WsCatalog.SchemaEntry describeRecord(Class<?> cls, Deque<Class<?>> pending) {
        final List<WsCatalog.FieldEntry> fields = new ArrayList<>();
        for (final RecordComponent component : cls.getRecordComponents()) {
            final String typeRef = describeFieldType(component.getGenericType(), pending);
            fields.add(new WsCatalog.FieldEntry(component.getName(), typeRef));
        }
        return new WsCatalog.SchemaEntry("record", fields, null);
    }

    private String describeFieldType(Type type, Deque<Class<?>> pending) {
        if (type instanceof Class<?> cls) {
            registerIfDescribable(cls, pending);
            return cls.getSimpleName();
        }
        if (type instanceof ParameterizedType parameterizedType) {
            final Class<?> raw = (Class<?>) parameterizedType.getRawType();
            final Type[] args = parameterizedType.getActualTypeArguments();
            final StringBuilder sb = new StringBuilder(raw.getSimpleName()).append("<");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(describeFieldType(args[i], pending));
            }
            return sb.append(">").toString();
        }
        return type.getTypeName();
    }

    private void registerIfDescribable(Class<?> cls, Set<Class<?>> referenced) {
        if (cls.isRecord() || cls.isEnum()) {
            referenced.add(cls);
        }
    }

    private void registerIfDescribable(Class<?> cls, Deque<Class<?>> pending) {
        if (cls.isRecord() || cls.isEnum()) {
            pending.push(cls);
        }
    }

    private WsCatalog.Envelope buildEnvelope() {
        final List<WsCatalog.FieldEntry> fields = List.of(
                new WsCatalog.FieldEntry("success", "boolean"),
                new WsCatalog.FieldEntry("data", "T"),
                new WsCatalog.FieldEntry("errorMessage", "String"),
                new WsCatalog.FieldEntry("id", "String")
        );
        return new WsCatalog.Envelope(
                properties.envelopeType() + "<T>",
                fields,
                "모든 토픽 페이로드는 이 envelope 으로 감싸 전송됩니다."
        );
    }

    private WsCatalog.Info toInfo(WsCatalogProperties.Info src) {
        if (src == null) {
            return new WsCatalog.Info(null, null, null);
        }
        return new WsCatalog.Info(src.title(), src.version(), src.description());
    }
}
