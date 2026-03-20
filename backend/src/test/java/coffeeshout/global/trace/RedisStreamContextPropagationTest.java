package coffeeshout.global.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.support.test.IntegrationTest;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Scope;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Redis Stream을 경유하는 이벤트의 Trace Context 전파를 검증하는 통합 테스트.
 *
 * <p>Publisher 스레드에서 생성한 traceId가 이벤트 DTO에 정상 주입되고,
 * Consumer 스레드에서 EventDispatcher가 TracerProvider.executeWithTraceContext()를
 * 실제로 호출하여 Span을 복원하는지 검증한다.</p>
 */
@IntegrationTest
class RedisStreamContextPropagationTest {

    @Autowired
    StreamPublisher streamPublisher;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    Tracer tracer;

    @MockitoSpyBean
    TracerProvider tracerProvider;

    private String joinCode;

    @BeforeEach
    void setUp() {
        Room testRoom = RoomFixture.호스트_꾹이();
        roomRepository.save(testRoom);
        joinCode = testRoom.getJoinCode().getValue();
    }

    @Nested
    class Observation_스코프_내에서_이벤트_발행_시 {

        @Test
        void Publisher의_traceId가_이벤트_DTO의_TraceInfo에_주입된다() {
            // given
            AtomicReference<String> publisherTraceId = new AtomicReference<>();
            AtomicReference<String> eventTraceId = new AtomicReference<>();

            Observation observation = Observation.createNotStarted("test-publish", observationRegistry).start();
            try (Scope scope = observation.openScope()) {
                publisherTraceId.set(tracer.currentSpan().context().traceId());

                RoomJoinEvent event = new RoomJoinEvent(joinCode, "전파테스트");
                eventTraceId.set(event.traceInfo().traceId());
            } finally {
                observation.stop();
            }

            // then
            assertThat(eventTraceId.get()).isNotBlank();
            assertThat(eventTraceId.get()).isEqualTo(publisherTraceId.get());
        }

        @Test
        void Consumer에서_TracerProvider_executeWithTraceContext가_호출된다() {
            // given
            AtomicReference<String> publisherTraceId = new AtomicReference<>();

            Observation observation = Observation.createNotStarted("test-e2e-trace", observationRegistry).start();
            try (Scope scope = observation.openScope()) {
                publisherTraceId.set(tracer.currentSpan().context().traceId());

                RoomJoinEvent event = new RoomJoinEvent(joinCode, "E2E전파");
                streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
            } finally {
                observation.stop();
            }

            // then — Consumer가 이벤트를 처리할 때까지 대기
            await().atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Room updatedRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow();
                        boolean playerExists = updatedRoom.getPlayers().stream()
                                .anyMatch(player -> "E2E전파".equals(player.getName().value()));
                        assertThat(playerExists).isTrue();
                    });

            // then — EventDispatcher가 TracerProvider.executeWithTraceContext()를
            // Publisher의 traceId가 담긴 TraceInfo로 실제 호출했는지 검증
            String expectedTraceId = publisherTraceId.get();
            verify(tracerProvider).executeWithTraceContext(
                    argThat(traceInfo -> expectedTraceId.equals(traceInfo.traceId())),
                    any(Runnable.class),
                    any()
            );
        }

        @Test
        void 여러_이벤트를_동시에_발행해도_각각의_traceId가_올바르게_전파된다() {
            // given
            String[] playerNames = {"동시전파1", "동시전파2", "동시전파3"};
            List<String> capturedTraceIds = new ArrayList<>();

            CountDownLatch startBarrier = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(playerNames.length);

            try {
                // when — CompletableFuture로 실제 동시 발행
                CompletableFuture<?>[] futures = new CompletableFuture[playerNames.length];
                for (int i = 0; i < playerNames.length; i++) {
                    String playerName = playerNames[i];
                    futures[i] = CompletableFuture.runAsync(() -> {
                        try {
                            startBarrier.await(3, java.util.concurrent.TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }

                        Observation observation = Observation.createNotStarted(
                                "test-concurrent-" + playerName, observationRegistry).start();
                        try (Scope scope = observation.openScope()) {
                            RoomJoinEvent event = new RoomJoinEvent(joinCode, playerName);

                            synchronized (capturedTraceIds) {
                                capturedTraceIds.add(event.traceInfo().traceId());
                            }

                            streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
                        } finally {
                            observation.stop();
                        }
                    }, executor);
                }

                startBarrier.countDown();
                CompletableFuture.allOf(futures).join();
            } finally {
                executor.shutdownNow();
            }

            // then — 각 이벤트가 서로 다른 traceId를 가짐
            assertThat(capturedTraceIds)
                    .hasSize(playerNames.length)
                    .allSatisfy(traceId -> assertThat(traceId).isNotBlank())
                    .doesNotHaveDuplicates();

            // then — 모든 이벤트가 Consumer에서 정상 처리됨
            await().atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Room updatedRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow();
                        for (String playerName : playerNames) {
                            boolean exists = updatedRoom.getPlayers().stream()
                                    .anyMatch(p -> playerName.equals(p.getName().value()));
                            assertThat(exists)
                                    .as("플레이어 '%s'가 Consumer에서 처리되어야 한다", playerName)
                                    .isTrue();
                        }
                    });

            // then — 각 이벤트에 대해 TracerProvider가 호출됨
            for (String traceId : capturedTraceIds) {
                verify(tracerProvider).executeWithTraceContext(
                        argThat(info -> traceId.equals(info.traceId())),
                        any(Runnable.class),
                        any()
                );
            }
        }
    }

    @Nested
    class Observation_스코프_바깥에서_이벤트_생성_시 {

        @Test
        void TraceInfo가_빈_값이다() {
            // given
            RoomJoinEvent event = new RoomJoinEvent(joinCode, "노트레이스");

            // then
            assertThat(event.traceInfo().traceId()).isEmpty();
            assertThat(event.traceInfo().spanId()).isEmpty();
            assertThat(event.traceInfo().traceable()).isFalse();
        }

        @Test
        void 빈_TraceInfo여도_Consumer에서_이벤트는_정상_처리된다() {
            // given
            RoomJoinEvent event = new RoomJoinEvent(joinCode, "노트레이스처리");
            assertThat(event.traceInfo().traceable()).isFalse();

            streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);

            // then — trace 없어도 비즈니스 로직은 돌아야 한다
            await().atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Room updatedRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow();
                        boolean exists = updatedRoom.getPlayers().stream()
                                .anyMatch(p -> "노트레이스처리".equals(p.getName().value()));
                        assertThat(exists).isTrue();
                    });

            // then — TracerProvider는 빈 TraceInfo로 호출되어야 하고,
            // 내부에서 traceable() == false이므로 Span 생성 없이 task만 실행
            verify(tracerProvider).executeWithTraceContext(
                    argThat(info -> !info.traceable()),
                    any(Runnable.class),
                    any()
            );
        }
    }
}
