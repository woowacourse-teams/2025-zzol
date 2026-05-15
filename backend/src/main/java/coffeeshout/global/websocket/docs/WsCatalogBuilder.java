package coffeeshout.global.websocket.docs;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

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
        final List<WsCatalog.SendEntry> sends = new ArrayList<>();
        final Set<Class<?>> referenced = new HashSet<>();

        final Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        for (final Object bean : beans.values()) {
            collectFromBean(bean, topics, sends, referenced);
        }

        topics.sort(Comparator.comparing(WsCatalog.TopicEntry::path));
        sends.sort(Comparator.comparing(WsCatalog.SendEntry::destination));

        final List<WsCatalog.TopicEntry> deduped = topics.stream()
                .collect(Collectors.toMap(
                        WsCatalog.TopicEntry::path,
                        t -> t,
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
                .values().stream().toList();

        final Map<String, WsCatalog.SchemaEntry> schemas = expandSchemas(referenced);

        return new WsCatalog(
                properties.stompEndpoint(),
                properties.appPath(),
                properties.topicPath(),
                properties.queuePath(),
                toInfo(properties.info()),
                buildEnvelope(),
                deduped,
                sends,
                schemas,
                new WsCatalog.ErrorShape(properties.errorTopic(), properties.envelopeType() + "<String>")
        );
    }

    private void collectFromBean(
            Object bean,
            List<WsCatalog.TopicEntry> topics,
            List<WsCatalog.SendEntry> sends,
            Set<Class<?>> referenced
    ) {
        final Class<?> targetClass = AopUtils.getTargetClass(bean);
        for (final Method method : targetClass.getDeclaredMethods()) {
            final WsTopic[] wsTopics = method.getAnnotationsByType(WsTopic.class);
            final MessageMapping messageMapping = AnnotationUtils.findAnnotation(method, MessageMapping.class);
            final WsReceive wsReceive = AnnotationUtils.findAnnotation(method, WsReceive.class);

            if (wsTopics.length == 0 && messageMapping == null && wsReceive == null) {
                continue;
            }
            final WsCatalog.Source source = new WsCatalog.Source(targetClass.getSimpleName(), method.getName());
            for (final WsTopic wsTopic : wsTopics) {
                topics.add(toTopicEntry(wsTopic, source, referenced));
            }
            if (messageMapping != null) {
                sends.add(toSendEntry(method, messageMapping, wsTopics, wsReceive, source, referenced));
            }
        }
    }

    private WsCatalog.TopicEntry toTopicEntry(WsTopic wsTopic, WsCatalog.Source source, Set<Class<?>> referenced) {
        final String fullPath = properties.topicPath() + wsTopic.path();
        final String payloadType = describePayloadType(wsTopic.payload(), wsTopic.generic(), referenced);
        return new WsCatalog.TopicEntry(fullPath, wsTopic.description(), payloadType, source);
    }

    private WsCatalog.SendEntry toSendEntry(
            Method method,
            MessageMapping mapping,
            WsTopic[] wsTopics,
            WsReceive wsReceive,
            WsCatalog.Source source,
            Set<Class<?>> referenced
    ) {
        final String[] values = mapping.value();
        final String mappingPath = values.length == 0 ? "" : values[0];
        final String fullDest = properties.appPath() + mappingPath;
        final String requestType = findRequestPayload(method, referenced);
        final List<String> triggersTopics = new ArrayList<>();
        for (final WsTopic wsTopic : wsTopics) {
            triggersTopics.add(properties.topicPath() + wsTopic.path());
        }
        if (wsReceive != null) {
            for (final String path : wsReceive.triggersTopics()) {
                triggersTopics.add(properties.topicPath() + path);
            }
        }
        final String description = wsTopics.length > 0 ? wsTopics[0].description()
                : wsReceive != null ? wsReceive.description()
                : "";
        return new WsCatalog.SendEntry(fullDest, description, requestType, triggersTopics, source);
    }

    private String describePayloadType(Class<?> payload, Class<?> generic, Set<Class<?>> referenced) {
        if (payload == Void.class) {
            return null;
        }
        if (generic == Void.class) {
            registerIfDomain(payload, referenced);
            return properties.envelopeType() + "<" + payload.getSimpleName() + ">";
        }
        registerIfDomain(generic, referenced);
        return properties.envelopeType() + "<" + payload.getSimpleName() + "<" + generic.getSimpleName() + ">>";
    }

    private String findRequestPayload(Method method, Set<Class<?>> referenced) {
        for (final java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(Payload.class)) {
                registerIfDomain(parameter.getType(), referenced);
                return parameter.getType().getSimpleName();
            }
        }
        for (final java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (!isFrameworkParameter(parameter)) {
                registerIfDomain(parameter.getType(), referenced);
                return parameter.getType().getSimpleName();
            }
        }
        return null;
    }

    private boolean isFrameworkParameter(java.lang.reflect.Parameter parameter) {
        if (parameter.isAnnotationPresent(DestinationVariable.class)) {
            return true;
        }
        final String pkg = parameter.getType().getPackageName();
        return pkg.startsWith("java.") || pkg.startsWith("javax.")
                || pkg.startsWith("jakarta.") || pkg.startsWith("org.springframework.");
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
            registerIfDomain(cls, pending);
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

    private void registerIfDomain(Class<?> cls, Set<Class<?>> referenced) {
        if (cls.isRecord() || cls.isEnum()) {
            referenced.add(cls);
        }
    }

    private void registerIfDomain(Class<?> cls, Deque<Class<?>> pending) {
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
