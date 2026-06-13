package coffeeshout.global.redis;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.gamecommon.RoomLifecycleEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RoomLifecycleEvent(중첩 sealed record)의 Redis Stream 직렬화 계약을 잠근다.
 *
 * <p>{@code BaseEvent}는 {@code @JsonTypeInfo(Id.NAME)}으로 {@code @type}을 싣고, {@code ObjectMapperConfig}가
 * Reflections로 {@code BaseEvent} 하위 타입을 자동 등록한다. 중첩 record는 평탄 클래스와 {@code @type} 파생·
 * Reflections 등록 경로가 다를 수 있어, 라운드트립으로 구체 타입 복원을 검증해 회귀를 막는다(ADR-0025 생명주기 이벤트).
 */
class RoomLifecycleEventSerializationTest {

    private final ObjectMapper mapper = new ObjectMapperConfig().redisObjectMapper();

    @Nested
    @DisplayName("BaseEvent로 역직렬화하면 구체 중첩 타입으로 복원된다")
    class RoundTrip {

        @Test
        @DisplayName("Created")
        void created() throws Exception {
            final RoomLifecycleEvent.Created event = new RoomLifecycleEvent.Created("호스트", "ABCD");

            final String json = mapper.writeValueAsString(event);
            final BaseEvent restored = mapper.readValue(json, BaseEvent.class);

            // 와이어 계약 고정: 중첩 record의 @type 식별자(Jackson Id.NAME 기본값 = getName 기반, 패키지만 제거).
            // 기존 평탄 GameRoomCreatedEvent는 "GameRoomCreatedEvent"였으므로 이 문자열은 신규 계약이다.
            assertThat(json).contains("\"@type\":\"RoomLifecycleEvent$Created\"");
            assertThat(restored)
                    .isInstanceOf(RoomLifecycleEvent.Created.class)
                    .isEqualTo(event);
        }

        @Test
        @DisplayName("Removed")
        void removed() throws Exception {
            final RoomLifecycleEvent.Removed event = new RoomLifecycleEvent.Removed("ABCD");

            final String json = mapper.writeValueAsString(event);
            final BaseEvent restored = mapper.readValue(json, BaseEvent.class);

            assertThat(restored)
                    .isInstanceOf(RoomLifecycleEvent.Removed.class)
                    .isEqualTo(event);
        }

        @Test
        @DisplayName("HostChanged")
        void hostChanged() throws Exception {
            final RoomLifecycleEvent.HostChanged event = new RoomLifecycleEvent.HostChanged("ABCD", "새호스트");

            final String json = mapper.writeValueAsString(event);
            final BaseEvent restored = mapper.readValue(json, BaseEvent.class);

            assertThat(restored)
                    .isInstanceOf(RoomLifecycleEvent.HostChanged.class)
                    .isEqualTo(event);
        }
    }
}
