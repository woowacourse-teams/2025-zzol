package coffeeshout.global.websocket.docs;

import coffeeshout.global.exception.custom.SystemException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prod")
public class WsCatalogBuilder implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;
    private final WsCatalogProperties properties;
    private volatile WsCatalog cached;
    private volatile String cachedEtag;

    public WsCatalogBuilder(ApplicationContext applicationContext, WsCatalogProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        build();
    }

    public WsCatalog build() {
        if (cached == null) {
            synchronized (this) {
                if (cached == null) {
                    final WsCatalog built = buildInternal();
                    cachedEtag = "\"" + Integer.toHexString(built.hashCode()) + "\"";
                    cached = built;
                }
            }
        }
        return cached;
    }

    public String getEtag() {
        build();
        return cachedEtag;
    }

    private WsCatalog buildInternal() {
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
            if (messageMapping != null && wsTopics.length == 0 && wsReceive == null) {
                log.warn("@MessageMapping 메서드에 @WsTopic 또는 @WsReceive 가 없습니다: {}#{}",
                        targetClass.getSimpleName(), method.getName());
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
        return new WsCatalog.TopicEntry(path, group.getFirst().payloadType(), buildPublishers(group), collectReferencedSchemas(group));
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
        return new WsCatalog.QueueEntry(path, group.getFirst().payloadType(), buildPublishers(group), collectReferencedSchemas(group));
    }

    private List<WsCatalog.Publisher> buildPublishers(List<? extends RawEntry> group) {
        return group.stream()
                .map(r -> new WsCatalog.Publisher(r.description(), r.source()))
                .sorted(Comparator.comparing(p -> p.source().className() + "#" + p.source().methodName()))
                .toList();
    }

    private List<String> collectReferencedSchemas(List<? extends RawEntry> group) {
        return group.stream()
                .flatMap(r -> r.referencedSchemas().stream())
                .distinct()
                .toList();
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
        validatePath("@WsQueue.path", wsQueue.path(), properties.userDestinationPrefix(), source);
        validatePayload("@WsQueue.payload", wsQueue.payload(), wsQueue.path(), source);
        if (wsQueue.generic() != Void.class) {
            validatePayload("@WsQueue.generic", wsQueue.generic(), wsQueue.path(), source);
        }
        final String fullPath = properties.userDestinationPrefix() + wsQueue.path();
        final String payloadType = describePayloadType(wsQueue.payload(), wsQueue.generic(), referenced);
        final List<String> refSchemas = collectPayloadSchemas(wsQueue.payload(), wsQueue.generic());
        return new RawQueue(fullPath, wsQueue.description(), payloadType, source, refSchemas);
    }

    private RawTopic toRawTopic(WsTopic wsTopic, WsCatalog.Source source, Set<Class<?>> referenced) {
        validatePath("@WsTopic.path", wsTopic.path(), properties.topicPath(), source);
        validatePayload("@WsTopic.payload", wsTopic.payload(), wsTopic.path(), source);
        if (wsTopic.generic() != Void.class) {
            validatePayload("@WsTopic.generic", wsTopic.generic(), wsTopic.path(), source);
        }
        final String fullPath = properties.topicPath() + wsTopic.path();
        final String payloadType = describePayloadType(wsTopic.payload(), wsTopic.generic(), referenced);
        final List<String> refSchemas = collectPayloadSchemas(wsTopic.payload(), wsTopic.generic());
        return new RawTopic(fullPath, wsTopic.description(), payloadType, source, refSchemas);
    }

    private void validatePath(String label, String path, String prefixToReject, WsCatalog.Source source) {
        if (path.isBlank()) {
            throw new SystemException(WsCatalogErrorCode.ANNOTATION_BLANK_PATH,
                    "%s 가 비어 있습니다: %s#%s".formatted(label, source.className(), source.methodName()));
        }
        if (!path.startsWith("/")) {
            throw new SystemException(WsCatalogErrorCode.ANNOTATION_INVALID_PATH_FORMAT,
                    "%s 는 '/' 로 시작해야 합니다: %s#%s path=%s".formatted(
                            label, source.className(), source.methodName(), path));
        }
        if (path.startsWith(prefixToReject + "/") || path.equals(prefixToReject)) {
            throw new SystemException(WsCatalogErrorCode.ANNOTATION_INVALID_PATH_FORMAT,
                    "%s 에 prefix('%s')가 이미 포함되어 있습니다 — 상대 경로만 입력하세요: %s#%s path=%s".formatted(
                            label, prefixToReject, source.className(), source.methodName(), path));
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
        final List<String> requestSchemas = findRequestSchemas(method);
        final List<String> triggersTopics = collectTriggerTopics(wsTopics, wsReceive, source);
        final String description = resolveDescription(wsTopics, wsReceive);
        final String[] values = mapping.value();
        final String[] destinations = values.length == 0 ? new String[]{""} : values;
        final List<WsCatalog.SendEntry> entries = new ArrayList<>();
        for (final String mappingPath : destinations) {
            entries.add(new WsCatalog.SendEntry(
                    properties.appPath() + mappingPath, description, requestType, triggersTopics, source, requestSchemas));
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
        registerIfDescribable(payload, referenced);
        final String envelopeName = properties.envelopeClass().getSimpleName();
        if (generic == Void.class) {
            return envelopeName + "<" + payload.getSimpleName() + ">";
        }
        registerIfDescribable(generic, referenced);
        return envelopeName + "<" + payload.getSimpleName() + "<" + generic.getSimpleName() + ">>";
    }

    private List<String> collectPayloadSchemas(Class<?> payload, Class<?> generic) {
        final Set<Class<?>> local = new LinkedHashSet<>();
        registerIfDescribable(payload, local);
        if (generic != Void.class) {
            registerIfDescribable(generic, local);
        }
        return local.stream().map(Class::getSimpleName).toList();
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

    private List<String> findRequestSchemas(Method method) {
        for (final Parameter parameter : method.getParameters()) {
            if (!parameter.isAnnotationPresent(Payload.class)) {
                continue;
            }
            final Class<?> type = parameter.getType();
            return (type.isRecord() || type.isEnum()) ? List.of(type.getSimpleName()) : List.of();
        }
        return List.of();
    }

    private Map<String, WsCatalog.SchemaEntry> expandSchemas(Set<Class<?>> seeds) {
        return new SchemaExpander().expand(seeds);
    }

    private class SchemaExpander {
        private final Map<String, WsCatalog.SchemaEntry> result = new LinkedHashMap<>();
        private final Map<String, Class<?>> seenByName = new LinkedHashMap<>();
        private final Deque<Class<?>> pending = new ArrayDeque<>();
        private final Set<Class<?>> visited = new HashSet<>();

        Map<String, WsCatalog.SchemaEntry> expand(Set<Class<?>> seeds) {
            seeds.stream().sorted(Comparator.comparing(Class::getName)).forEach(pending::addLast);
            while (!pending.isEmpty()) {
                process(pending.pop());
            }
            return result.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        }

        private void process(Class<?> cls) {
            if (!visited.add(cls)) {
                return;
            }
            final String simpleName = cls.getSimpleName();
            if (seenByName.containsKey(simpleName)) {
                log.warn("스키마 simpleName 충돌: '{}' — {} vs {} (첫 선언 유지)",
                        simpleName, seenByName.get(simpleName).getName(), cls.getName());
                return;
            }
            seenByName.put(simpleName, cls);
            result.put(simpleName, describe(cls));
        }

        private WsCatalog.SchemaEntry describe(Class<?> cls) {
            if (cls.isEnum()) {
                return describeEnum(cls);
            }
            if (cls.isRecord()) {
                return describeRecord(cls);
            }
            return new WsCatalog.SchemaEntry(WsCatalog.SchemaKind.OBJECT, List.of(), null);
        }

        private WsCatalog.SchemaEntry describeEnum(Class<?> cls) {
            final List<String> values = new ArrayList<>();
            for (final Object constant : cls.getEnumConstants()) {
                values.add(((Enum<?>) constant).name());
            }
            return new WsCatalog.SchemaEntry(WsCatalog.SchemaKind.ENUM, null, values);
        }

        private WsCatalog.SchemaEntry describeRecord(Class<?> cls) {
            final List<WsCatalog.FieldEntry> fields = new ArrayList<>();
            for (final RecordComponent component : cls.getRecordComponents()) {
                fields.add(
                        new WsCatalog.FieldEntry(component.getName(), describeFieldType(component.getGenericType())));
            }
            return new WsCatalog.SchemaEntry(WsCatalog.SchemaKind.RECORD, fields, null);
        }

        private String describeFieldType(Type type) {
            registerTypes(type);
            return typeNameOf(type);
        }

        private void registerTypes(Type type) {
            if (type instanceof Class<?> cls) {
                registerIfDescribable(cls, pending);
                return;
            }
            if (type instanceof ParameterizedType pt) {
                Arrays.stream(pt.getActualTypeArguments()).forEach(this::registerTypes);
            }
        }
    }

    private static String typeNameOf(Type type) {
        if (type instanceof Class<?> cls) {
            return cls.getSimpleName();
        }
        if (type instanceof ParameterizedType pt) {
            final String args = Arrays.stream(pt.getActualTypeArguments())
                    .map(WsCatalogBuilder::typeNameOf)
                    .collect(Collectors.joining(", "));
            return ((Class<?>) pt.getRawType()).getSimpleName() + "<" + args + ">";
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
        final List<WsCatalog.FieldEntry> fields = new ArrayList<>();
        for (final RecordComponent component : envelopeClass.getRecordComponents()) {
            fields.add(new WsCatalog.FieldEntry(component.getName(), typeNameOf(component.getGenericType())));
        }
        return new WsCatalog.Envelope(
                envelopeClass.getSimpleName() + "<T>",
                fields,
                "모든 토픽 페이로드는 이 envelope 으로 감싸 전송됩니다."
        );
    }

    private sealed interface RawEntry permits RawTopic, RawQueue {
        String path();
        String description();
        String payloadType();
        WsCatalog.Source source();
        List<String> referencedSchemas();
    }

    private record RawTopic(String path, String description, String payloadType, WsCatalog.Source source, List<String> referencedSchemas)
            implements RawEntry {
    }

    private record RawQueue(String path, String description, String payloadType, WsCatalog.Source source, List<String> referencedSchemas)
            implements RawEntry {
    }
}
