package coffeeshout.websocket.docs;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import coffeeshout.global.exception.custom.SystemException;
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
        final List<RawTopic> rawTopics = new ArrayList<>();
        final List<RawQueue> rawQueues = new ArrayList<>();
        final List<WsCatalog.SendEntry> sends = new ArrayList<>();
        final Set<Class<?>> referenced = new HashSet<>();

        final Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        for (final Object bean : beans.values()) {
            collectFromBean(bean, rawTopics, rawQueues, sends, referenced);
        }

        final List<WsCatalog.TopicEntry> topics = mergeTopics(rawTopics);
        final List<WsCatalog.QueueEntry> queues = mergeQueues(rawQueues);
        sends.sort(Comparator.comparing(WsCatalog.SendEntry::destination));

        final Map<String, WsCatalog.SchemaEntry> schemas = expandSchemas(referenced);

        return new WsCatalog(
                properties.stompEndpoint(),
                properties.appPath(),
                properties.topicPath(),
                properties.queuePath(),
                buildEnvelope(),
                topics,
                queues,
                sends,
                schemas,
                new WsCatalog.ErrorShape(properties.errorTopic(), properties.envelopeClass().getSimpleName() + "<String>")
        );
    }

    private void collectFromBean(
            Object bean,
            List<RawTopic> rawTopics,
            List<RawQueue> rawQueues,
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
                rawTopics.add(toRawTopic(wsTopic, source, referenced));
            }
            for (final WsQueue wsQueue : wsQueues) {
                rawQueues.add(toRawQueue(wsQueue, source, referenced));
            }
            if (messageMapping != null) {
                sends.addAll(toSendEntries(method, messageMapping, wsTopics, wsReceive, source, referenced));
            }
        }
    }

    private List<WsCatalog.TopicEntry> mergeTopics(List<RawTopic> raw) {
        final Map<String, List<RawTopic>> byPath = raw.stream()
                .collect(Collectors.groupingBy(RawTopic::path, LinkedHashMap::new, Collectors.toList()));
        final List<WsCatalog.TopicEntry> result = new ArrayList<>();
        for (final Map.Entry<String, List<RawTopic>> entry : byPath.entrySet()) {
            result.add(mergeTopicGroup(entry.getKey(), entry.getValue()));
        }
        result.sort(Comparator.comparing(WsCatalog.TopicEntry::path));
        return result;
    }

    private WsCatalog.TopicEntry mergeTopicGroup(String path, List<RawTopic> group) {
        warnIfPayloadConflict("토픽", path, group);
        final List<WsCatalog.Publisher> publishers = group.stream()
                .map(r -> new WsCatalog.Publisher(r.description(), r.source()))
                .toList();
        return new WsCatalog.TopicEntry(path, group.getFirst().payloadType(), publishers);
    }

    private List<WsCatalog.QueueEntry> mergeQueues(List<RawQueue> raw) {
        final Map<String, List<RawQueue>> byPath = raw.stream()
                .collect(Collectors.groupingBy(RawQueue::path, LinkedHashMap::new, Collectors.toList()));
        final List<WsCatalog.QueueEntry> result = new ArrayList<>();
        for (final Map.Entry<String, List<RawQueue>> entry : byPath.entrySet()) {
            result.add(mergeQueueGroup(entry.getKey(), entry.getValue()));
        }
        result.sort(Comparator.comparing(WsCatalog.QueueEntry::path));
        return result;
    }

    private WsCatalog.QueueEntry mergeQueueGroup(String path, List<RawQueue> group) {
        warnIfPayloadConflict("큐", path, group);
        final List<WsCatalog.Publisher> publishers = group.stream()
                .map(r -> new WsCatalog.Publisher(r.description(), r.source()))
                .toList();
        return new WsCatalog.QueueEntry(path, group.getFirst().payloadType(), publishers);
    }

    private void warnIfPayloadConflict(String kind, String path, List<? extends RawEntry> group) {
        final Set<String> payloads = group.stream().map(RawEntry::payloadType).collect(Collectors.toSet());
        if (payloads.size() <= 1) {
            return;
        }
        final List<String> sources = group.stream()
                .map(r -> r.source() + " → " + r.payloadType())
                .toList();
        log.warn("동일 {} path 에 서로 다른 payload 가 선언되었습니다: path={}, sources={}", kind, path, sources);
    }

    private List<String> collectTriggerTopics(WsTopic[] wsTopics, WsReceive wsReceive, WsCatalog.Source source) {
        final Stream<String> fromTopics = Arrays.stream(wsTopics)
                .map(t -> properties.topicPath() + t.path());
        final Stream<String> fromReceive = Optional.ofNullable(wsReceive)
                .stream()
                .flatMap(r -> Arrays.stream(r.respondsOnTopics()))
                .map(path -> validatedTopicPath(path, source));
        return Stream.concat(fromTopics, fromReceive).toList();
    }

    private String validatedTopicPath(String path, WsCatalog.Source source) {
        if (path == null || path.isBlank()) {
            throw new SystemException(WsCatalogErrorCode.ANNOTATION_BLANK_TOPIC_PATH,
                    "@WsReceive.respondsOnTopics 에 빈 경로가 포함되어 있습니다: %s#%s".formatted(
                            source.className(), source.methodName()));
        }
        if (!path.startsWith("/")) {
            throw new SystemException(WsCatalogErrorCode.ANNOTATION_INVALID_PATH_FORMAT,
                    "@WsReceive.respondsOnTopics 는 '/' 로 시작해야 합니다: %s#%s path=%s".formatted(
                            source.className(), source.methodName(), path));
        }
        return properties.topicPath() + path;
    }

    private RawQueue toRawQueue(WsQueue wsQueue, WsCatalog.Source source, Set<Class<?>> referenced) {
        validatePath("@WsQueue.path", wsQueue.path(), source);
        validatePayload("@WsQueue.payload", wsQueue.payload(), wsQueue.path(), source);
        final String fullPath = properties.userDestinationPrefix() + wsQueue.path();
        final String payloadType = describePayloadType(wsQueue.payload(), wsQueue.generic(), referenced);
        return new RawQueue(fullPath, wsQueue.description(), payloadType, source);
    }

    private RawTopic toRawTopic(WsTopic wsTopic, WsCatalog.Source source, Set<Class<?>> referenced) {
        validatePath("@WsTopic.path", wsTopic.path(), source);
        validatePayload("@WsTopic.payload", wsTopic.payload(), wsTopic.path(), source);
        final String fullPath = properties.topicPath() + wsTopic.path();
        final String payloadType = describePayloadType(wsTopic.payload(), wsTopic.generic(), referenced);
        return new RawTopic(fullPath, wsTopic.description(), payloadType, source);
    }

    private void validatePath(String label, String path, WsCatalog.Source source) {
        if (path.isBlank()) {
            throw new SystemException(WsCatalogErrorCode.ANNOTATION_BLANK_PATH,
                    "%s 가 비어 있습니다: %s#%s".formatted(label, source.className(), source.methodName()));
        }
        if (!path.startsWith("/")) {
            throw new SystemException(WsCatalogErrorCode.ANNOTATION_INVALID_PATH_FORMAT,
                    "%s 는 '/' 로 시작해야 합니다: %s#%s path=%s".formatted(
                            label, source.className(), source.methodName(), path));
        }
    }

    private void validatePayload(String label, Class<?> payload, String path, WsCatalog.Source source) {
        if (payload == Void.class) {
            throw new SystemException(WsCatalogErrorCode.ANNOTATION_VOID_PAYLOAD,
                    "%s 가 Void.class 입니다: %s#%s path=%s".formatted(
                            label, source.className(), source.methodName(), path));
        }
        if (payload == Object.class) {
            throw new SystemException(WsCatalogErrorCode.ANNOTATION_OBJECT_PAYLOAD,
                    "%s 에 Object.class 는 허용되지 않습니다 — @WsReceive 를 사용하거나 명시적 payload 타입을 지정하세요: %s#%s path=%s"
                            .formatted(label, source.className(), source.methodName(), path));
        }
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
            entries.add(new WsCatalog.SendEntry(
                    properties.appPath() + mappingPath, description, requestType, triggersTopics, source));
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
        if (generic == Void.class) {
            registerIfDescribable(payload, referenced);
            return properties.envelopeClass().getSimpleName() + "<" + payload.getSimpleName() + ">";
        }
        registerIfDescribable(payload, referenced);
        registerIfDescribable(generic, referenced);
        return properties.envelopeClass().getSimpleName() + "<" + payload.getSimpleName() + "<" + generic.getSimpleName() + ">>";
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

    private void registerIfDescribable(Class<?> cls, Collection<Class<?>> target) {
        if (cls.isRecord() || cls.isEnum()) {
            target.add(cls);
        }
    }

    private WsCatalog.Envelope buildEnvelope() {
        final Class<?> envelopeClass = properties.envelopeClass();
        if (!envelopeClass.isRecord()) {
            throw new SystemException(WsCatalogErrorCode.INVALID_ENVELOPE_CLASS,
                    "envelope-class 는 record 타입이어야 합니다: " + envelopeClass.getName());
        }
        final WsCatalog.SchemaEntry schema = describeRecord(envelopeClass, new ArrayDeque<>());
        return new WsCatalog.Envelope(
                envelopeClass.getSimpleName() + "<T>",
                schema.fields(),
                "모든 토픽 페이로드는 이 envelope 으로 감싸 전송됩니다."
        );
    }

    private sealed interface RawEntry permits RawTopic, RawQueue {
        String payloadType();
        WsCatalog.Source source();
    }

    private record RawTopic(String path, String description, String payloadType, WsCatalog.Source source)
            implements RawEntry {
    }

    private record RawQueue(String path, String description, String payloadType, WsCatalog.Source source)
            implements RawEntry {
    }
}
