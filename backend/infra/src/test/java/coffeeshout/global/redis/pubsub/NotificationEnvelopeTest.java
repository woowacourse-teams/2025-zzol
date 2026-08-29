package coffeeshout.global.redis.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("알림 봉투")
class NotificationEnvelopeTest {

    private static final String TRACEPARENT = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("직렬화 왕복에서 destination·payload·traceparent가 보존된다")
    void 직렬화_왕복에서_세_필드가_보존된다() throws Exception {
        // given
        final NotificationEnvelope envelope = new NotificationEnvelope(
                "/topic/room/ABC123/gameState",
                "{\"success\":true,\"data\":{\"state\":\"PLAYING\"}}",
                TRACEPARENT
        );

        // when
        final String json = objectMapper.writeValueAsString(envelope);
        final NotificationEnvelope 왕복한_봉투 = objectMapper.readValue(json, NotificationEnvelope.class);

        // then
        assertThat(왕복한_봉투).isEqualTo(envelope);
    }

    @Test
    @DisplayName("traceparent가 없는 봉투도 왕복한다")
    void traceparent가_없는_봉투도_왕복한다() throws Exception {
        // given
        final NotificationEnvelope envelope =
                new NotificationEnvelope("/topic/room/ABC123", "{\"success\":true}", null);

        // when
        final NotificationEnvelope 왕복한_봉투 =
                objectMapper.readValue(objectMapper.writeValueAsString(envelope), NotificationEnvelope.class);

        // then
        assertThat(왕복한_봉투.traceparent()).isNull();
        assertThat(왕복한_봉투.destination()).isEqualTo("/topic/room/ABC123");
        assertThat(왕복한_봉투.payload()).isEqualTo("{\"success\":true}");
    }

    @Test
    @DisplayName("payload는 텍스트로 실려 왕복 후에도 바이트가 그대로다")
    void payload는_텍스트로_실려_바이트가_보존된다() throws Exception {
        // given — 필드 순서까지 그대로여야 수신 인스턴스들의 메시지 식별자가 같아진다
        final String payloadJson = "{\"success\":true,\"data\":{\"b\":2,\"a\":1},\"errorMessage\":null}";
        final NotificationEnvelope envelope = new NotificationEnvelope("/topic/room/ABC123", payloadJson, null);

        // when
        final NotificationEnvelope 왕복한_봉투 =
                objectMapper.readValue(objectMapper.writeValueAsString(envelope), NotificationEnvelope.class);

        // then
        assertThat(왕복한_봉투.payload()).isEqualTo(payloadJson);
    }
}
