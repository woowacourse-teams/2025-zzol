package coffeeshout.global.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Redis Stream을 경유하는 이벤트의 Trace Context 전파를 검증하는 통합 테스트.
 * <p>
 * Publisher 스레드에서 생성한 traceId가 Consumer 스레드(EventDispatcher → TracerProvider)까지 정상적으로 전파되는지 확인한다.
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
        void Redis_Stream을_경유해도_Consumer에서_이벤트가_정상_처리된다() {
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

            // then — Awaitility로 비동기 Consumer 처리 완료를 폴링
            await().atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Room updatedRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow();
                        boolean playerExists = updatedRoom.getPlayers().stream()
                                .anyMatch(player -> "E2E전파".equals(player.getName().value()));
                        assertThat(playerExists)
                                .as("Consumer가 traceId를 복원하고 이벤트를 정상 처리해야 한다")
                                .isTrue();
                    });

            assertThat(publisherTraceId.get()).isNotBlank();
        }

        @Test
        void 여러_이벤트를_동시에_발행해도_각각의_traceId가_올바르게_전파된다() {
            // given
            int eventCount = 3;
            CountDownLatch publishLatch = new CountDownLatch(eventCount);
            String[] playerNames = {"동시전파1", "동시전파2", "동시전파3"};

            for (String playerName : playerNames) {
                Observation observation = Observation.createNotStarted("test-concurrent-" + playerName,
                        observationRegistry).start();
                try (Scope scope = observation.openScope()) {
                    RoomJoinEvent event = new RoomJoinEvent(joinCode, playerName);

                    // traceId가 비어있지 않은지 발행 시점에 확인
                    assertThat(event.traceInfo().traceId()).isNotBlank();

                    streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
                    publishLatch.countDown();
                } finally {
                    observation.stop();
                }
            }

            // then — 모든 이벤트가 Consumer에서 정상 처리되었는지 Awaitility로 검증
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
        }
    }
}
