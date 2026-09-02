package coffeeshout.contract;

import coffeeshout.websocket.docs.WsCatalog;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WebSocket 계약 TS 생성기")
class WsContractTsEmitterTest {

    @Test
    @DisplayName("prefix 를 떼고 경로 변수를 ${string} 으로 바꾸며 빈 union 은 never 로 낸다")
    void prefix_를_떼고_경로_변수를_치환한다() {
        final WsCatalog catalog = new WsCatalog(
                "/ws",
                "/app",
                "/topic",
                "/queue",
                null,
                List.of(new WsCatalog.TopicEntry("/topic/room/{joinCode:.{4}}/x", "T", List.of(), List.of())),
                List.of(new WsCatalog.QueueEntry("/user/queue/friends/presence", "T", List.of(), List.of())),
                List.of(),
                Map.of(),
                new WsCatalog.ErrorShape("/queue/errors", "T"));

        final String ts = WsContractTsEmitter.emit(catalog);

        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(ts).contains("  | `/room/${string}/x`;");
        softly.assertThat(ts).contains("  | '/user/queue/friends/presence';");
        softly.assertThat(ts).contains("  | '/user/queue/errors';");
        softly.assertThat(ts).contains("export type WsSendPath = never;");
        softly.assertAll();
    }
}
