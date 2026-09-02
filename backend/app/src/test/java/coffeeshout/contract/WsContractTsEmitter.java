package coffeeshout.contract;

import coffeeshout.websocket.docs.WsCatalog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 카탈로그를 FE 가 import 하는 TypeScript 타입으로 낸다.
 *
 * <p>FE 훅은 prefix 없는 destination 을 받고 스스로 {@code /topic}·{@code /app} 을 붙인다
 * ({@code useWebSocketMessaging.ts}). 그래서 topic 과 send 는 prefix 를 떼고, broker 가 직접 라우팅하는
 * {@code /user/queue/...} 는 그대로 둔다. 경로 변수 세그먼트는 {@code ${string}} 이 되어 호출부의
 * 템플릿 리터럴이 contextual typing 으로 그대로 통과한다.
 */
final class WsContractTsEmitter {

    private static final String HEADER = """
            // 자동 생성 파일이라 손으로 고치지 않는다. 원천은 backend 의 @WsTopic/@WsQueue/@WsReceive 다.
            // 갱신: backend/gradlew -p backend :app:test --tests '*WsCatalogContractTest*'
            // destination 은 방 코드를 변수로 보간해서 넘긴다. `/room/ABCD/winner` 처럼 통째로 고정한
            // 문자열은 정상 경로여도 아래 동치 검사에 걸려 컴파일 오류가 난다.

            """;

    /**
     * {@code ${string}} 은 {@code /} 도 삼켜서 {@code `/room/${string}`} 하나가 모든 room 경로에 맞아
     * 버린다. union 대입만으로는 오타를 못 잡으므로, 호출부 리터럴이 정확히 한 패턴과 같을 때(상호 대입)만
     * 통과시키는 타입을 함께 낸다. 어긋나면 틀린 경로가 찍힌 문자열 리터럴 타입이 되어 컴파일 오류가 난다.
     */
    private static final String FOOTER = """

            // `${string}` 은 '/' 도 삼키므로 `/room/${string}` 이 모든 room 경로에 맞아 버린다.
            // 호출부 리터럴이 정확히 한 패턴과 같을 때(상호 대입)만 통과시킨다. 아니면 오류 메시지에 경로를 찍는다.
            type Same<A, B> = [A] extends [B] ? ([B] extends [A] ? true : false) : false;
            type MatchesOne<D, P> = P extends unknown ? Same<D, P> : never;
            type Exact<D extends string, P extends string> = true extends MatchesOne<D, P>
              ? D
              : `ws 카탈로그에 없는 destination: ${D}`;

            export type WsSubscribeDestination<D extends WsSubscribePath> = Exact<D, WsSubscribePath>;
            export type WsSendDestination<D extends WsSendPath> = Exact<D, WsSendPath>;

            """;

    private static final String PAYLOAD_NOTE = """
            // BE record 를 그대로 옮긴 payload 타입. BE 에서 @Nullable 을 단 필드만 `field?: T | null` 이다.
            // @JsonInclude(NON_NULL) 이면 필드가 빠지고, 아니면 null 이 오므로 둘 다 허용한다.
            """;

    private static final String PAYLOAD_OF_NOTE = """

            // destination 별 payload. 세그먼트가 많은 패턴을 앞에 둬야 `/room/${string}` 이 다른 room 경로를 삼키지 않는다.
            """;

    private static final Set<String> NUMBERS = Set.of("int", "long", "double", "Integer", "Long", "Double");
    private static final Set<String> STRINGS = Set.of("String", "Instant");

    private WsContractTsEmitter() {}

    static String emit(WsCatalog catalog) {
        final List<String> topics = catalog.topics().stream()
                .map(topic -> stripPrefix(topic.path(), catalog.topicPrefix()))
                .toList();
        final List<String> queues =
                catalog.queues().stream().map(WsCatalog.QueueEntry::path).toList();
        final List<String> sends = catalog.sends().stream()
                .map(send -> stripPrefix(send.destination(), catalog.app()))
                .toList();
        final String errors = catalog.errors().topic();

        return HEADER
                + "/** 구독 destination. useWebSocketSubscription 이 /topic 을 붙이므로 topic 은 prefix 없이 쓴다. */\n"
                + union("WsTopicPath", topics)
                + "\n"
                + "/** 개인 큐. broker 가 직접 라우팅하므로 /user/queue 를 그대로 쓴다. */\n"
                + union("WsQueuePath", queues)
                + "\n"
                + "export type WsSubscribePath =\n"
                + "  | WsTopicPath\n"
                + "  | WsQueuePath\n"
                + "  | " + literal("/user" + errors) + ";\n"
                + "\n"
                + "/** 송신 destination. send 가 /app 을 붙이므로 prefix 없이 쓴다. */\n"
                + union("WsSendPath", sends)
                + FOOTER
                + PAYLOAD_NOTE
                + schemas(catalog.schemas())
                + PAYLOAD_OF_NOTE
                + payloadOf(catalog);
    }

    private static String schemas(Map<String, WsCatalog.SchemaEntry> schemas) {
        final StringBuilder out = new StringBuilder();
        new TreeMap<>(schemas).forEach((name, entry) -> {
            out.append("export type ").append(name).append(" = ");
            switch (entry.kind()) {
                case ENUM ->
                    out.append(entry.values().stream()
                            .map(value -> "'" + value + "'")
                            .collect(Collectors.joining(" | ")));
                case RECORD -> {
                    out.append("{\n");
                    entry.fields().forEach(field -> {
                        final boolean nullable = field.type().endsWith("?");
                        final String type = nullable
                                ? field.type().substring(0, field.type().length() - 1)
                                : field.type();
                        out.append("  ")
                                .append(field.name())
                                .append(nullable ? "?: " : ": ")
                                .append(tsType(type, schemas.keySet()))
                                .append(nullable ? " | null;\n" : ";\n");
                    });
                    out.append("}");
                }
                case OBJECT -> out.append("Record<string, unknown>");
            }
            out.append(";\n");
        });
        return out.toString();
    }

    /**
     * FE 형태의 destination 을 payload 타입에 잇는 조건부 타입 체인. {@code ${string}} 이 {@code /} 를 삼키므로
     * 세그먼트 수 내림차순, 같으면 리터럴 세그먼트 수 내림차순으로 정렬해 짧은 패턴이 긴 경로를 가로채지 않게 한다.
     */
    private static String payloadOf(WsCatalog catalog) {
        final Set<String> names = catalog.schemas().keySet();
        final List<String[]> entries = new ArrayList<>();
        catalog.topics()
                .forEach(topic -> entries.add(new String[] {
                    stripPrefix(topic.path(), catalog.topicPrefix()), unwrapEnvelope(topic.payloadType())
                }));
        catalog.queues()
                .forEach(queue -> entries.add(new String[] {queue.path(), unwrapEnvelope(queue.payloadType())}));
        entries.add(new String[] {
            "/user" + catalog.errors().topic(), unwrapEnvelope(catalog.errors().payloadType())
        });

        entries.sort(Comparator.<String[]>comparingInt(e -> -e[0].split("/", -1).length)
                .thenComparingInt(e -> -literalSegments(e[0]))
                .thenComparing(e -> e[0]));

        return "export type WsPayloadOf<D extends WsSubscribePath> =\n"
                + entries.stream()
                        .map(e -> "  D extends " + literal(e[0]) + " ? " + tsType(e[1], names) + " :\n")
                        .collect(Collectors.joining())
                + "  never;\n";
    }

    private static int literalSegments(String path) {
        return (int) Arrays.stream(path.split("/", -1))
                .filter(segment -> !segment.contains("{"))
                .count();
    }

    private static String unwrapEnvelope(String payloadType) {
        final String prefix = "WebSocketResponse<";
        return payloadType.startsWith(prefix) && payloadType.endsWith(">")
                ? payloadType.substring(prefix.length(), payloadType.length() - 1)
                : payloadType;
    }

    /** Java 타입 문자열을 TS 타입으로 옮긴다. 제네릭은 카탈로그가 쓰는 {@code List<T>}·{@code Map<K, V>} 만 다룬다. */
    static String tsType(String javaType, Set<String> schemaNames) {
        final String type = javaType.trim();
        if (type.startsWith("List<") && type.endsWith(">")) {
            return tsType(type.substring(5, type.length() - 1), schemaNames) + "[]";
        }
        if (type.startsWith("Map<") && type.endsWith(">")) {
            final String inner = type.substring(4, type.length() - 1);
            // JSON 객체 키는 문자열이라 Map 키 타입은 제네릭을 못 가진다. 첫 콤마가 곧 최상위 콤마다.
            return "Record<string, " + tsType(inner.substring(inner.indexOf(',') + 1), schemaNames) + ">";
        }
        if (NUMBERS.contains(type)) {
            return "number";
        }
        if (type.equals("boolean") || type.equals("Boolean")) {
            return "boolean";
        }
        if (STRINGS.contains(type)) {
            return "string";
        }
        return schemaNames.contains(type) ? type : "unknown";
    }

    private static String union(String name, Collection<String> paths) {
        if (paths.isEmpty()) {
            return "export type " + name + " = never;\n";
        }
        return "export type " + name + " =\n"
                + paths.stream().sorted().map(path -> "  | " + literal(path)).collect(Collectors.joining("\n"))
                + ";\n";
    }

    /** 경로 변수 세그먼트({@code {joinCode}}, {@code {joinCode:.{4}}})는 통째로 {@code ${string}} 이 된다. */
    private static String literal(String path) {
        final String template = Arrays.stream(path.split("/", -1))
                .map(segment -> segment.contains("{") ? "${string}" : segment)
                .collect(Collectors.joining("/"));
        return template.contains("${") ? "`" + template + "`" : "'" + template + "'";
    }

    private static String stripPrefix(String path, String prefix) {
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }
}
