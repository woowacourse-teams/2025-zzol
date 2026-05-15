package coffeeshout.global.websocket.docs;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.springframework.stereotype.Controller;

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
                "/ws",
                "/queue/errors",
                "WebSocketResponse",
                "http://localhost:8080/ws",
                new WsCatalogProperties.Info("테스트 타이틀", "1.0.0", "테스트 설명")
        );
        builder = new WsCatalogBuilder(applicationContext, properties);
    }

    @Nested
    @DisplayName("@WsTopic 와 @MessageMapping 이 함께 선언된 컨트롤러")
    class WsTopic_과_MessageMapping_이_함께_선언된_컨트롤러 {

        @Test
        @DisplayName("토픽과 send 모두 카탈로그에 포함되고 send 는 triggersTopics 가 채워진다")
        void 토픽과_send_가_모두_포함된다() {
            when(applicationContext.getBeansWithAnnotation(eq(Component.class)))
                    .thenReturn(Map.of("fixture", new FixtureWebSocketController()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.topics()).hasSize(1);
                softly.assertThat(catalog.topics().get(0).path()).isEqualTo("/topic/test/{joinCode}/result");
                softly.assertThat(catalog.topics().get(0).payloadType())
                        .isEqualTo("WebSocketResponse<FixturePayload>");
                softly.assertThat(catalog.topics().get(0).source().className()).isEqualTo("FixtureWebSocketController");
                softly.assertThat(catalog.topics().get(0).source().methodName()).isEqualTo("doAction");

                softly.assertThat(catalog.sends()).hasSize(1);
                softly.assertThat(catalog.sends().get(0).destination()).isEqualTo("/app/test/{joinCode}/action");
                softly.assertThat(catalog.sends().get(0).requestType()).isEqualTo("FixtureRequest");
                softly.assertThat(catalog.sends().get(0).triggersTopics())
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
            when(applicationContext.getBeansWithAnnotation(eq(Component.class)))
                    .thenReturn(Map.of("fixture", new FixtureReceiveController()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.topics()).isEmpty();
                softly.assertThat(catalog.sends()).hasSize(1);
                softly.assertThat(catalog.sends().get(0).destination()).isEqualTo("/app/test/{joinCode}/command");
                softly.assertThat(catalog.sends().get(0).triggersTopics())
                        .containsExactly("/topic/test/{joinCode}/result");
                softly.assertThat(catalog.sends().get(0).requestType()).isEqualTo("FixtureRequest");
            });
        }
    }

    @Nested
    @DisplayName("List 형태의 generic payload 가 선언된 메서드")
    class List_형태의_generic_payload_가_선언된_메서드 {

        @Test
        @DisplayName("payloadType 이 WebSocketResponse<List<X>> 형식으로 표기된다")
        void 제네릭이_표기된다() {
            when(applicationContext.getBeansWithAnnotation(eq(Component.class)))
                    .thenReturn(Map.of("fixture", new FixtureGenericPublisher()));

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.topics()).hasSize(1);
                softly.assertThat(catalog.topics().get(0).payloadType())
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
            when(applicationContext.getBeansWithAnnotation(eq(Component.class)))
                    .thenReturn(Map.of());

            final WsCatalog catalog = builder.build();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(catalog.envelope().type()).isEqualTo("WebSocketResponse<T>");
                softly.assertThat(catalog.envelope().fields()).extracting(WsCatalog.FieldEntry::name)
                        .containsExactly("success", "data", "errorMessage", "id");
                softly.assertThat(catalog.errors().topic()).isEqualTo("/queue/errors");
                softly.assertThat(catalog.info().title()).isEqualTo("테스트 타이틀");
            });
        }
    }

    @Nested
    @DisplayName("동일 메서드에 @WsTopic 이 여러 개 선언된 Publisher")
    class 동일_메서드에_WsTopic_이_여러_개_선언된_Publisher {

        @Test
        @DisplayName("선언한 수만큼 TopicEntry 가 생성된다")
        void 토픽_여러_개가_생성된다() {
            when(applicationContext.getBeansWithAnnotation(eq(Component.class)))
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
    @DisplayName("잘못된 @WsTopic 어노테이션")
    class 잘못된_WsTopic_어노테이션 {

        @Test
        @DisplayName("path 가 비어 있으면 빌드가 실패한다")
        void path_가_비어_있으면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(eq(Component.class)))
                    .thenReturn(Map.of("fixture", new FixtureBlankPathPublisher()));

            assertThatThrownBy(() -> builder.build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("@WsTopic.path");
        }

        @Test
        @DisplayName("payload 가 Void.class 이면 빌드가 실패한다")
        void payload_가_Void_이면_실패한다() {
            when(applicationContext.getBeansWithAnnotation(eq(Component.class)))
                    .thenReturn(Map.of("fixture", new FixtureVoidPayloadPublisher()));

            assertThatThrownBy(() -> builder.build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("@WsTopic.payload");
        }
    }

    @Controller
    static class FixtureWebSocketController {

        @MessageMapping("/test/{joinCode}/action")
        @WsTopic(path = "/test/{joinCode}/result", payload = FixturePayload.class, description = "테스트 토픽")
        public void doAction(@DestinationVariable String joinCode, @Payload FixtureRequest request) {
        }
    }

    @Controller
    static class FixtureReceiveController {

        @MessageMapping("/test/{joinCode}/command")
        @WsReceive(respondsOnTopics = "/test/{joinCode}/result", description = "테스트 수신 엔드포인트")
        public void handleCommand(@DestinationVariable String joinCode, @Payload FixtureRequest request) {
        }
    }

    @Controller
    static class FixtureGenericPublisher {

        @WsTopic(
                path = "/test/{joinCode}/list",
                payload = List.class,
                generic = FixturePayload.class
        )
        public void publishList() {
        }
    }

    @Component
    static class FixtureMultiTopicPublisher {

        @WsTopic(path = "/test/a", payload = FixturePayload.class)
        @WsTopic(path = "/test/b", payload = FixturePayload.class)
        public void publish() {
        }
    }

    @Component
    static class FixtureBlankPathPublisher {

        @WsTopic(path = "", payload = FixturePayload.class)
        public void publish() {
        }
    }

    @Component
    static class FixtureVoidPayloadPublisher {

        @WsTopic(path = "/test/void", payload = Void.class)
        public void publish() {
        }
    }

    public record FixturePayload(String name, int value) {
    }

    public record FixtureRequest(String input) {
    }
}
