package coffeeshout.global.redis;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.BaseEventDummy;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventTypeNameTest {

    @Test
    @DisplayName("평탄(top-level) 이벤트는 단순 클래스명을 쓴다")
    void 평탄_이벤트는_단순_클래스명() {
        assertThat(EventTypeName.of(BaseEventDummy.class)).isEqualTo("BaseEventDummy");
    }

    @Test
    @DisplayName("중첩 이벤트는 외부 클래스로 한정해 모호성을 없앤다")
    void 중첩_이벤트는_외부_클래스로_한정() {
        final BaseEvent event = new Family.Child("id", Instant.EPOCH);

        assertThat(EventTypeName.of(event)).isEqualTo("Family.Child");
    }

    sealed interface Family extends BaseEvent {
        record Child(String eventId, Instant timestamp) implements Family {
        }
    }
}
