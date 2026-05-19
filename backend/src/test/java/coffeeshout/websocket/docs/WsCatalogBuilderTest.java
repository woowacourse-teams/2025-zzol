package coffeeshout.websocket.docs;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import coffeeshout.websocket.ui.WebSocketResponse;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

class WsCatalogBuilderTest {

    private ApplicationContext applicationContext;
    private WsCatalogProperties properties;
    private WsCatalogBuilder builder;

    @BeforeEach
    void setUp() {
        applicationContext = mock(ApplicationContext.class);
        properties = new WsCatalogProperties(
                "/app",
                "/topic",
                "/queue",
                "/user",
                "/ws",
                "/queue/errors",
                WebSocketResponse.class,
                List.of("127.0.0.1")
        );
        builder = new WsCatalogBuilder(applicationContext, properties);
    }

    @Nested
    @DisplayName("@WsTopic 와 @MessageMapping 이 함께 선언된 컨트롤러")
    class WsTopic_과_MessageMapping_이_함께_선언된_컨트롤러 {

        @Test
        @DisplayName("토픽과 send 모두 카탈로그에 포함되고 send 는 triggersTopics 가 채워진다")
        void 토픽과_send_가_모두_포함된다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureWebSocketController()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.topics()).hasSize(1);
                softly.assertThat(catalog.topics().getFirst().path()).isEqualTo("/topic/test/{joinCode}/result");
                softly.assertThat(catalog.topics().getFirst().payloadType())
                        .isEqualTo("WebSocketResponse<FixturePayload>");
                softly.assertThat(catalog.topics().getFirst().publishers()).hasSize(1);
                softly.assertThat(catalog.topics().getFirst().publishers().getFirst().description())
                        .isEqualTo("테스트 토픽");
                softly.assertThat(catalog.topics().getFirst().publishers().getFirst().source().className())
                        .isEqualTo("FixtureWebSocketController");
                softly.assertThat(catalog.topics().getFirst().publishers().getFirst().source().methodName())
                        .isEqualTo("doAction");

                softly.assertThat(catalog.sends()).hasSize(1);
                softly.assertThat(catalog.sends().getFirst().destination()).isEqualTo("/app/test/{joinCode}/action");
                softly.assertThat(catalog.sends().getFirst().requestType()).isEqualTo("FixtureRequest");
                softly.assertThat(catalog.sends().getFirst().triggersTopics())
                        .containsExactly("/topic/test/{joinCode}/result");

                softly.assertThat(catalog.schemas()).containsKeys("FixturePayload", "FixtureRequest");
            });
        }
    }

    @Nested
    @DisplayName("@WsReceive 가 선언된 컨트롤러")
    class WsReceive_가_선언된_컨트롤러 {

        @Test
        @DisplayName("TopicEntry 없이 SendEntry 만 생성되고 triggersTopics 가 채워진다")
        void send_만_포함되고_triggersTopics_가_채워진다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureReceiveController()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.topics()).isEmpty();
                softly.assertThat(catalog.sends()).hasSize(1);
                softly.assertThat(catalog.sends().getFirst().destination()).isEqualTo("/app/test/{joinCode}/command");
                softly.assertThat(catalog.sends().getFirst().triggersTopics())
                        .containsExactly("/topic/test/{joinCode}/result");
                softly.assertThat(catalog.sends().getFirst().requestType()).isEqualTo("FixtureRequest");
            });
        }
    }

    @Nested
    @DisplayName("List 형태의 generic payload 가 선언된 메서드")
    class List_형태의_generic_payload_가_선언된_메서드 {

        @Test
        @DisplayName("payloadType 이 WebSocketResponse<List<X>> 형식으로 표기된다")
        void 제네릭이_표기된다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureGenericPublisher()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.topics()).hasSize(1);
                softly.assertThat(catalog.topics().getFirst().payloadType())
                        .isEqualTo("WebSocketResponse<List<FixturePayload>>");
                softly.assertThat(catalog.schemas()).containsKey("FixturePayload");
            });
        }
    }

    @Nested
    @DisplayName("Envelope 메타데이터")
    class Envelope_메타데이터 {

        @Test
        @DisplayName("WebSocketResponse 필드 4개가 포함된다")
        void envelope_필드가_채워진다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of());

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.envelope().type()).isEqualTo("WebSocketResponse<T>");
                softly.assertThat(catalog.envelope().fields()).extracting(WsCatalog.FieldEntry::name)
                        .containsExactly("success", "data", "errorMessage", "id");
                softly.assertThat(catalog.errors().topic()).isEqualTo("/queue/errors");
            });
        }
    }

    @Nested
    @DisplayName("동일 메서드에 @WsTopic 이 여러 개 선언된 Publisher")
    class 동일_메서드에_WsTopic_이_여러_개_선언된_Publisher {

        @Test
        @DisplayName("선언한 수만큼 TopicEntry 가 생성된다")
        void 토픽_여러_개가_생성된다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureMultiTopicPublisher()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.topics()).hasSize(2);
                softly.assertThat(catalog.topics())
                        .extracting(WsCatalog.TopicEntry::path)
                        .containsExactlyInAnyOrder("/topic/test/a", "/topic/test/b");
            });
        }
    }

    @Nested
    @DisplayName("@WsQueue 가 선언된 Notifier")
    class WsQueue_가_선언된_Notifier {

        @Test
        @DisplayName("queues 에 엔트리가 생성되고 path 에 /user prefix 가 붙는다")
        void 큐_엔트리가_생성된다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureQueueNotifier()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.queues()).hasSize(1);
                softly.assertThat(catalog.queues().getFirst().path()).isEqualTo("/user/queue/friends/requests");
                softly.assertThat(catalog.queues().getFirst().payloadType())
                        .isEqualTo("WebSocketResponse<FixturePayload>");
                softly.assertThat(catalog.queues().getFirst().publishers()).hasSize(1);
                softly.assertThat(catalog.queues().getFirst().publishers().getFirst().description())
                        .isEqualTo("친구 요청 알림");
                softly.assertThat(catalog.topics()).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("동일 path 의 @WsQueue 가 여러 메서드에 선언된 Notifier")
    class 동일_path의_WsQueue가_여러_메서드에_선언된_Notifier {

        @Test
        @DisplayName("하나의 QueueEntry 로 묶이고 publishers 에 각 메서드의 description 이 보존된다")
        void 다중_발행자가_보존된다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureDuplicateQueueNotifier()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.queues()).hasSize(1);
                softly.assertThat(catalog.queues().getFirst().path()).isEqualTo("/user/queue/friends/responses");
                softly.assertThat(catalog.queues().getFirst().publishers())
                        .extracting(WsCatalog.Publisher::description)
                        .containsExactlyInAnyOrder("수락", "거절");
                softly.assertThat(catalog.queues().getFirst().publishers())
                        .extracting(p -> p.source().methodName())
                        .containsExactlyInAnyOrder("onAccepted", "onRejected");
            });
        }
    }

    @Nested
    @DisplayName("동일 path 의 @WsTopic 이 여러 Publisher 에 선언된 경우")
    class 동일_path의_WsTopic이_여러_Publisher에_선언된_경우 {

        @Test
        @DisplayName("하나의 TopicEntry 로 묶이고 publishers 에 각 메서드가 보존된다")
        void 다중_발행자가_보존된다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureSamePathTopicPublisher()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.topics()).hasSize(1);
                softly.assertThat(catalog.topics().getFirst().path()).isEqualTo("/topic/test/state");
                softly.assertThat(catalog.topics().getFirst().publishers())
                        .extracting(p -> p.source().methodName())
                        .containsExactlyInAnyOrder("publishStart", "publishFinish");
            });
        }
    }

    @Nested
    @DisplayName("generic payload 가 선언된 @WsQueue")
    class generic_payload가_선언된_WsQueue {

        @Test
        @DisplayName("payloadType 이 WebSocketResponse<List<X>> 형식으로 표기된다")
        void generic이_표기된다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureGenericQueueNotifier()));

            final WsCatalog catalog = builder.build();

            assertThat(catalog.queues().getFirst().payloadType())
                    .isEqualTo("WebSocketResponse<List<FixturePayload>>");
        }
    }

    @Nested
    @DisplayName("잘못된 @WsReceive 어노테이션")
    class 잘못된_WsReceive_어노테이션 {

        @Test
        @DisplayName("respondsOnTopics 경로가 '/' 로 시작하지 않으면 빌드가 실패한다")
        void respondsOnTopics_가_슬래시로_시작하지_않으면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureNoSlashReceiveController()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_INVALID_PATH_FORMAT);
        }
    }

    @Nested
    @DisplayName("잘못된 @WsQueue 어노테이션")
    class 잘못된_WsQueue_어노테이션 {

        @Test
        @DisplayName("path 가 비어 있으면 빌드가 실패한다")
        void path_가_비어_있으면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureBlankPathQueueNotifier()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_BLANK_PATH);
        }

        @Test
        @DisplayName("payload 가 Void.class 이면 빌드가 실패한다")
        void payload_가_Void_이면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureVoidPayloadQueueNotifier()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_VOID_PAYLOAD);
        }

        @Test
        @DisplayName("payload 가 Object.class 이면 빌드가 실패한다")
        void payload_가_Object_이면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureObjectPayloadQueueNotifier()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_OBJECT_PAYLOAD);
        }

        @Test
        @DisplayName("path 가 '/' 로 시작하지 않으면 빌드가 실패한다")
        void path_가_슬래시로_시작하지_않으면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureNoSlashPathQueueNotifier()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_INVALID_PATH_FORMAT);
        }
    }

    @Nested
    @DisplayName("잘못된 @WsTopic 어노테이션")
    class 잘못된_WsTopic_어노테이션 {

        @Test
        @DisplayName("path 가 비어 있으면 빌드가 실패한다")
        void path_가_비어_있으면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureBlankPathPublisher()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_BLANK_PATH);
        }

        @Test
        @DisplayName("payload 가 Void.class 이면 빌드가 실패한다")
        void payload_가_Void_이면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureVoidPayloadPublisher()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_VOID_PAYLOAD);
        }

        @Test
        @DisplayName("payload 가 Object.class 이면 빌드가 실패한다")
        void payload_가_Object_이면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureObjectPayloadPublisher()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_OBJECT_PAYLOAD);
        }

        @Test
        @DisplayName("generic 이 Object.class 이면 빌드가 실패한다")
        void generic_이_Object_이면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(eq(Component.class)))
                    .thenReturn(Map.of("fixture", new FixtureObjectGenericPublisher()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_OBJECT_PAYLOAD);
        }

        @Test
        @DisplayName("path 가 '/' 로 시작하지 않으면 빌드가 실패한다")
        void path_가_슬래시로_시작하지_않으면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(Component.class))
                    .thenReturn(Map.of("fixture", new FixtureNoSlashPathPublisher()));

            assertCoffeeShoutException(() -> builder.build(), WsCatalogErrorCode.ANNOTATION_INVALID_PATH_FORMAT);
        }
    }

    @Nested
    @DisplayName("envelope-class 검증")
    class envelope_class_검증 {

        @Test
        @DisplayName("envelope-class 가 record 가 아니면 빌드가 실패한다")
        void envelope_class_가_record_가_아니면_실패한다() {
            final WsCatalogProperties invalidProperties = new WsCatalogProperties(
                    "/app", "/topic", "/queue", "/user", "/ws", "/queue/errors",
                    NonRecordEnvelope.class,
                    List.of("127.0.0.1")
            );
            final WsCatalogBuilder invalidBuilder = new WsCatalogBuilder(applicationContext, invalidProperties);
            when(applicationContext.getBeansWithAnnotation(eq(Component.class))).thenReturn(Map.of());

            assertCoffeeShoutException(() -> invalidBuilder.build(), WsCatalogErrorCode.INVALID_ENVELOPE_CLASS);
        }

        static class NonRecordEnvelope {
        }
    }

    static class FixtureQueueNotifier {

        @WsQueue(path = "/queue/friends/requests", payload = FixturePayload.class, description = "친구 요청 알림")
        public void onRequest() {
        }
    }

    static class FixtureDuplicateQueueNotifier {

        @WsQueue(path = "/queue/friends/responses", payload = FixturePayload.class, description = "수락")
        public void onAccepted() {
        }

        @WsQueue(path = "/queue/friends/responses", payload = FixturePayload.class, description = "거절")
        public void onRejected() {
        }
    }

    static class FixtureGenericQueueNotifier {

        @WsQueue(path = "/queue/friends/list", payload = List.class, generic = FixturePayload.class)
        public void onListChanged() {
        }
    }

    static class FixtureWebSocketController {

        @MessageMapping("/test/{joinCode}/action")
        @WsTopic(path = "/test/{joinCode}/result", payload = FixturePayload.class, description = "테스트 토픽")
        public void doAction(@DestinationVariable String joinCode, @Payload FixtureRequest request) {
        }
    }

    static class FixtureReceiveController {

        @MessageMapping("/test/{joinCode}/command")
        @WsReceive(respondsOnTopics = "/test/{joinCode}/result", description = "테스트 수신 엔드포인트")
        public void handleCommand(@DestinationVariable String joinCode, @Payload FixtureRequest request) {
        }
    }

    static class FixtureGenericPublisher {

        @WsTopic(
                path = "/test/{joinCode}/list",
                payload = List.class,
                generic = FixturePayload.class
        )
        public void publishList() {
        }
    }

    static class FixtureMultiTopicPublisher {

        @WsTopic(path = "/test/a", payload = FixturePayload.class)
        @WsTopic(path = "/test/b", payload = FixturePayload.class)
        public void publish() {
        }
    }

    static class FixtureSamePathTopicPublisher {

        @WsTopic(path = "/test/state", payload = FixturePayload.class, description = "시작")
        public void publishStart() {
        }

        @WsTopic(path = "/test/state", payload = FixturePayload.class, description = "종료")
        public void publishFinish() {
        }
    }

    static class FixtureObjectPayloadPublisher {

        @WsTopic(path = "/test/object", payload = Object.class)
        public void publish() {
        }
    }

    static class FixtureObjectGenericPublisher {

        @WsTopic(path = "/test/obj-generic", payload = List.class, generic = Object.class)
        public void publish() {
        }
    }

    static class FixtureObjectPayloadQueueNotifier {

        @WsQueue(path = "/queue/object", payload = Object.class)
        public void onEvent() {
        }
    }

    static class FixtureBlankPathQueueNotifier {

        @WsQueue(path = "", payload = FixturePayload.class)
        public void onEvent() {
        }
    }

    static class FixtureVoidPayloadQueueNotifier {

        @WsQueue(path = "/queue/test", payload = Void.class)
        public void onEvent() {
        }
    }

    static class FixtureBlankPathPublisher {

        @WsTopic(path = "", payload = FixturePayload.class)
        public void publish() {
        }
    }

    static class FixtureVoidPayloadPublisher {

        @WsTopic(path = "/test/void", payload = Void.class)
        public void publish() {
        }
    }

    static class FixtureNoSlashPathPublisher {

        @WsTopic(path = "test/result", payload = FixturePayload.class)
        public void publish() {
        }
    }

    static class FixtureNoSlashPathQueueNotifier {

        @WsQueue(path = "queue/test", payload = FixturePayload.class)
        public void onEvent() {
        }
    }

    static class FixtureNoSlashReceiveController {

        @MessageMapping("/test/{joinCode}/command")
        @WsReceive(respondsOnTopics = "test/{joinCode}/result")
        public void handleCommand(@Payload FixtureRequest request) {
        }
    }

    public record FixturePayload(String name, int value) {
    }

    public record FixtureRequest(String input) {
    }
}
