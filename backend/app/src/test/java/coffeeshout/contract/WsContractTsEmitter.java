package coffeeshout.contract;

import coffeeshout.websocket.docs.WsCatalog;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
                + FOOTER;
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
