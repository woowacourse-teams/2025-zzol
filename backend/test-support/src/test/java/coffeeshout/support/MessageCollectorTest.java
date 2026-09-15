package coffeeshout.support;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.TestStompSession.MessageCollector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MessageCollector duration")
class MessageCollectorTest {

    private static final long ARRIVAL_GAP_MS = 500L;
    private static final long TEST_THREAD_LAG_MS = 200L;

    private final ScheduledExecutorService server = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void tearDown() {
        server.shutdownNow();
    }

    /**
     * 게임 IT 는 duration 을 "직전 메시지 → 이번 메시지 간격" 으로 단언한다. get() 을 부른 시점부터 잰다면
     * 테스트 스레드가 앞 메시지를 처리하느라 늦게 부를수록 값이 줄어 CI 부하에서 흔들린다(#1782).
     */
    @Test
    void duration_은_get_호출_시점이_아니라_메시지_도착_간격을_잰다() {
        final MessageCollector collector = new MessageCollector();
        collector.add("first");
        server.schedule(() -> collector.add("second"), ARRIVAL_GAP_MS, TimeUnit.MILLISECONDS);

        collector.get();
        Awaitility.await().pollDelay(TEST_THREAD_LAG_MS, TimeUnit.MILLISECONDS).until(() -> true);
        final MessageResponse second = collector.get();

        assertThat(second.payload()).isEqualTo("second");
        assertThat(second.duration())
                .as("테스트 스레드가 %dms 늦게 get() 을 불러도 도착 간격 %dms 가 나와야 한다", TEST_THREAD_LAG_MS, ARRIVAL_GAP_MS)
                .isBetween(ARRIVAL_GAP_MS - 50, ARRIVAL_GAP_MS + 500);
    }
}
