package coffeeshout.global.websocket.docs;

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
                true,
                "coffeeshout",
                "/app",
                "/topic",
                "http://localhost:8080/ws",
                new WsCatalogProperties.Info("테스트 타이틀", "1.0.0", "테스트 설명")
        );
        builder = new WsCatalogBuilder(applicationContext, properties);
    }

    @Nested
    @DisplayName("@WsTopic 와 @MessageMapping 이 함께 선언된 컨트롤러")
    class WhenControllerHasBothAnnotations {

        @Test
        @DisplayName("토픽과 send 모두 카탈로그에 포함되고 send 는 triggersTopic 이 채워진다")
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
    @DisplayName("List 형태의 generic payload 가 선언된 메서드")
    class WhenGenericIsPresent {

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
    class Envelope {

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

    @Controller
    static class FixtureWebSocketController {

        @MessageMapping("/test/{joinCode}/action")
        @WsTopic(path = "/test/{joinCode}/result", payload = FixturePayload.class, description = "테스트 토픽")
        public void doAction(@DestinationVariable String joinCode, FixtureRequest request) {
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

    public record FixturePayload(String name, int value) {
    }

    public record FixtureRequest(String input) {
    }
}
