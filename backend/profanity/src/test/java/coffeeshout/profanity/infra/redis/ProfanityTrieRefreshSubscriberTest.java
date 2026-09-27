package coffeeshout.profanity.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.profanity.application.ProfanityFilterService;
import coffeeshout.profanity.config.ProfanityTrieRebuildProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 트라이 재빌드 신호 병합·최소 간격 회귀 테스트(#1759).
 *
 * <p>10만 건 실측에서 트라이 재빌드가 10,028회, 합계 2,272초 돌아 회차 시간(271초)의 8.4배가 겹쳐 돌았다.
 * 리스너 컨테이너 기본 실행기가 신호마다 스레드를 새로 만들어 동시 실행 상한이 없었던 게 원인이다.
 */
class ProfanityTrieRefreshSubscriberTest {

    private static final Message MESSAGE = mock(Message.class);

    private RedisMessageListenerContainer container;
    private ProfanityFilterService filterService;
    private SimpleMeterRegistry meterRegistry;
    private StubClock clock;
    private ProfanityTrieRefreshSubscriber subscriber;

    @BeforeEach
    void setUp() {
        container = mock(RedisMessageListenerContainer.class);
        filterService = mock(ProfanityFilterService.class);
        meterRegistry = new SimpleMeterRegistry();
        clock = new StubClock(Instant.parse("2026-09-06T00:00:00Z"));
    }

    @AfterEach
    void tearDown() {
        subscriber.shutdown();
    }

    private void 구독자를_준비한다(Duration minInterval) {
        subscriber = new ProfanityTrieRefreshSubscriber(
                container, filterService, meterRegistry, clock, new ProfanityTrieRebuildProperties(minInterval));
        subscriber.register();
    }

    @Nested
    @DisplayName("신호 병합")
    class 신호_병합 {

        @Test
        @DisplayName("연속 신호는 재빌드를 신호 횟수만큼 돌리지 않는다")
        void 연속_신호가_병합된다() {
            구독자를_준비한다(Duration.ZERO);

            for (int i = 0; i < 20; i++) {
                subscriber.onMessage(MESSAGE, null);
            }

            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(meterRegistry
                            .get("profanity.trie.rebuild.coalesced")
                            .counter()
                            .count())
                    .as("신호 20개 중 일부가 병합돼 버려진다")
                    .isGreaterThan(0));
            // 신호 20개가 재빌드 20회가 되려면 매 반복 사이에 재빌드가 끝나야 하는데,
            // 그러면 위 병합 단언이 먼저 깨진다. 워커 속도와 무관한 안전한 상한이다.
            verify(filterService, atMost(19)).rebuildTrie();
        }
    }

    @Nested
    @DisplayName("동시 실행")
    class 동시_실행 {

        @Test
        @DisplayName("신호 기반 재빌드는 동시 실행이 1을 넘지 않는다")
        void 신호_기반_재빌드는_동시_실행이_1을_넘지_않는다() throws InterruptedException {
            구독자를_준비한다(Duration.ZERO);
            final AtomicInteger concurrent = new AtomicInteger(0);
            final AtomicInteger maxConcurrent = new AtomicInteger(0);
            final CountDownLatch releaseRebuild = new CountDownLatch(1);
            doAnswer(invocation -> {
                        final int current = concurrent.incrementAndGet();
                        maxConcurrent.updateAndGet(max -> Math.max(max, current));
                        releaseRebuild.await(); // 테스트가 풀어줄 때까지, 딱 그만큼만 붙잡는다.
                        concurrent.decrementAndGet();
                        return null;
                    })
                    .when(filterService)
                    .rebuildTrie();

            // 10개 스레드가 동시에 신호를 보내도 CAS가 승자를 하나로 정한다.
            final ExecutorService signalSenders = Executors.newFixedThreadPool(10);
            for (int i = 0; i < 10; i++) {
                signalSenders.execute(() -> subscriber.onMessage(MESSAGE, null));
            }
            signalSenders.shutdown();
            signalSenders.awaitTermination(5, TimeUnit.SECONDS);

            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> assertThat(concurrent.get()).isEqualTo(1));

            // 플래그는 DB를 읽기 전에 이미 내려가 있어 이 신호가 두 번째 재빌드를 예약한다.
            // 실행기 스레드가 하나뿐이라 첫 재빌드가 끝나기 전에는 절대 함께 실행되지 않는다.
            subscriber.onMessage(MESSAGE, null);

            releaseRebuild.countDown();
            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> assertThat(concurrent.get()).isZero());
            assertThat(maxConcurrent.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("재빌드 도중 신호")
    class 재빌드_도중_신호 {

        @Test
        @DisplayName("DB를 읽는 동안 들어온 신호는 재빌드를 한 번 더 유발한다")
        void 읽기_도중_신호가_재빌드를_한번_더_유발한다() throws InterruptedException {
            구독자를_준비한다(Duration.ZERO);
            final CountDownLatch enteredRebuild = new CountDownLatch(1);
            final CountDownLatch releaseRebuild = new CountDownLatch(1);
            doAnswer(invocation -> {
                        enteredRebuild.countDown();
                        releaseRebuild.await();
                        return null;
                    })
                    .when(filterService)
                    .rebuildTrie();

            subscriber.onMessage(MESSAGE, null);
            assertThat(enteredRebuild.await(2, TimeUnit.SECONDS))
                    .as("첫 재빌드가 DB를 읽는 중이다")
                    .isTrue();

            // 플래그가 읽기 전에 내려가 있어야 이 신호가 다음 재빌드를 예약한다.
            subscriber.onMessage(MESSAGE, null);
            releaseRebuild.countDown();

            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> verify(filterService, times(2)).rebuildTrie());
        }
    }

    @Nested
    @DisplayName("최소 간격")
    class 최소_간격 {

        @Test
        @DisplayName("간격 안에 들어온 신호는 간격이 지난 뒤에 반영된다")
        void 간격_안_신호는_지연되어_반영된다() {
            final Duration minInterval = Duration.ofSeconds(1);
            구독자를_준비한다(minInterval);

            subscriber.onMessage(MESSAGE, null);
            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> verify(filterService, times(1)).rebuildTrie());

            subscriber.onMessage(MESSAGE, null);
            // 간격이 지나기 전이라 두 번째 신호는 아직 반영되지 않는다.
            verify(filterService, times(1)).rebuildTrie();

            await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> verify(filterService, times(2)).rebuildTrie());
        }

        @Test
        @DisplayName("간격이 지난 뒤 신호는 대기 없이 반영된다")
        void 간격이_지난_신호는_즉시_반영된다() {
            final Duration minInterval = Duration.ofSeconds(5);
            구독자를_준비한다(minInterval);

            subscriber.onMessage(MESSAGE, null);
            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> verify(filterService, times(1)).rebuildTrie());

            // 실제 시간은 그대로 두고 주입된 Clock만 간격 이상으로 흘려보낸다.
            clock.advance(minInterval.plusSeconds(1));
            subscriber.onMessage(MESSAGE, null);

            await().atMost(Duration.ofMillis(500))
                    .untilAsserted(() -> verify(filterService, times(2)).rebuildTrie());
        }

        @Test
        @DisplayName("시계가 뒤로 뛰어도 지연은 최소 간격을 넘지 않는다")
        void 시계가_뒤로_뛰어도_지연은_최소_간격을_넘지_않는다() {
            final Duration minInterval = Duration.ofMillis(300);
            구독자를_준비한다(minInterval);

            subscriber.onMessage(MESSAGE, null);
            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> verify(filterService, times(1)).rebuildTrie());

            // NTP 스텝 조정 등으로 시계가 뒤로 뛰는 상황을 흉내낸다. 클램프가 없으면 남은 시간이
            // minInterval + 되돌린 폭(10초)이 되어 아래 대기 시간 안에 두 번째 재빌드가 끝나지 않는다.
            clock.advance(Duration.ofSeconds(10).negated());
            subscriber.onMessage(MESSAGE, null);

            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> verify(filterService, times(2)).rebuildTrie());
        }
    }

    @Nested
    @DisplayName("재빌드 실패")
    class 재빌드_실패 {

        @Test
        @DisplayName("재빌드가 예외를 던져도 다음 신호는 정상 처리된다")
        void 예외_후_다음_신호가_정상_처리된다() {
            구독자를_준비한다(Duration.ZERO);
            doThrow(new RuntimeException("DB 조회 실패"))
                    .doNothing()
                    .when(filterService)
                    .rebuildTrie();

            subscriber.onMessage(MESSAGE, null);
            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(meterRegistry
                            .get("profanity.trie.rebuild.failure")
                            .counter()
                            .count())
                    .isEqualTo(1));

            subscriber.onMessage(MESSAGE, null);
            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> verify(filterService, times(2)).rebuildTrie());
        }
    }

    @Nested
    @DisplayName("종료")
    class 종료 {

        @Test
        @DisplayName("종료 후 신호가 와도 예외가 새지 않는다")
        void 종료_후_신호는_예외없이_무시된다() {
            구독자를_준비한다(Duration.ZERO);
            subscriber.shutdown();

            assertThatNoException().isThrownBy(() -> subscriber.onMessage(MESSAGE, null));
        }
    }

    /** 재빌드 최소 간격 계산을 흉내내기 위한 수동 진행 시계. */
    private static final class StubClock extends Clock {

        private Instant now;

        private StubClock(Instant start) {
            this.now = start;
        }

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
