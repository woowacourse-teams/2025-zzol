package coffeeshout.global.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.global.redis.stream.StreamRecordFields;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.support.app.IntegrationTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Redis Stream을 경유하는 이벤트의 Trace Context 전파를 검증하는 통합 테스트.
 *
 * <p>Publisher 스레드의 트레이스 컨텍스트가 레코드의 traceparent 필드로 주입되고,
 * Consumer 스레드에서 StreamTracePropagator가 캐리어에서 컨텍스트를 복원해
 * consumer span 스코프 안에서 처리하는지 검증한다.</p>
 *
 * <p>{@code @SpringBootTest}는 기본으로 {@code management.tracing.enabled=false}를 주입해
 * Propagator가 noop이 되므로, {@code @AutoConfigureObservability}로 실제 W3C 전파를 활성화한다.
 * OTLP 익스포터의 로컬 전송 시도는 무해하다 (export 실패는 경고 로그만 남긴다).</p>
 */
@AutoConfigureObservability
class RedisStreamContextPropagationTest extends IntegrationTestSupport {

    @Autowired
    StreamPublisher streamPublisher;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    Tracer tracer;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("redisObjectMapper")
    ObjectMapper redisObjectMapper;

    @MockitoSpyBean
    StreamTracePropagator streamTracePropagator;

    private String joinCode;

    @BeforeEach
    void setUp() {
        Room testRoom = RoomFixture.호스트_꾹이();
        roomRepository.save(testRoom);
        joinCode = testRoom.getJoinCode().getValue();
    }

    /**
     * 발행된 레코드를 찾아 traceparent 필드 값을 반환한다.
     * 레코드 자체가 없으면 검증 실패, traceparent 필드만 없으면 null을 반환한다.
     */
    private String findPublishedTraceparent(String guestName) {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                .range(RoomStreamKey.BROADCAST.getRedisKey(), Range.unbounded());

        MapRecord<String, Object, Object> published = records.stream()
                .filter(record -> {
                    Object payload = record.getValue().get(StreamRecordFields.PAYLOAD);
                    return payload != null && payload.toString().contains(guestName);
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("발행된 레코드를 찾지 못했습니다: " + guestName));

        Object traceparent = published.getValue().get(StreamRecordFields.TRACEPARENT);
        return traceparent == null ? null : traceparent.toString();
    }

    private void awaitPlayerJoined(String guestName) {
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Room updatedRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow();
                    boolean playerExists = updatedRoom.getPlayers().stream()
                            .anyMatch(player -> guestName.equals(player.getName().value()));
                    assertThat(playerExists).isTrue();
                });
    }

    @Nested
    class Observation_스코프_내에서_이벤트_발행_시 {

        @Test
        void 레코드의_traceparent_필드에_Publisher의_traceId가_주입된다() {
            // given
            AtomicReference<String> publisherTraceId = new AtomicReference<>();

            Observation observation = Observation.createNotStarted("test-publish", observationRegistry).start();
            try (Scope scope = observation.openScope()) {
                publisherTraceId.set(tracer.currentSpan().context().traceId());

                RoomJoinEvent event = new RoomJoinEvent(joinCode, "전파테스트");
                streamPublisher.publish(RoomStreamKey.BROADCAST, event);
            } finally {
                observation.stop();
            }

            // then — 레코드에 W3C traceparent 캐리어 필드가 실린다
            String traceparent = findPublishedTraceparent("전파테스트");
            assertThat(traceparent).isNotBlank();
            assertThat(traceparent).contains(publisherTraceId.get());
        }

        @Test
        void Consumer에서_Publisher의_traceparent가_담긴_캐리어로_consumer_span이_복원된다() {
            // given
            AtomicReference<String> publisherTraceId = new AtomicReference<>();

            Observation observation = Observation.createNotStarted("test-e2e-trace", observationRegistry).start();
            try (Scope scope = observation.openScope()) {
                publisherTraceId.set(tracer.currentSpan().context().traceId());

                RoomJoinEvent event = new RoomJoinEvent(joinCode, "E2E전파");
                streamPublisher.publish(RoomStreamKey.BROADCAST, event);
            } finally {
                observation.stop();
            }

            // then — Consumer가 이벤트를 처리할 때까지 대기
            awaitPlayerJoined("E2E전파");

            // then — StreamTracePropagator가 Publisher의 traceId가 담긴 캐리어로
            // consumer span 스코프 실행을 수행했는지 검증
            String expectedTraceId = publisherTraceId.get();
            verify(streamTracePropagator).runInConsumerScope(
                    argThat(carrier -> {
                        String traceparent = carrier.get(StreamRecordFields.TRACEPARENT);
                        return traceparent != null && traceparent.contains(expectedTraceId);
                    }),
                    eq("RoomJoinEvent"),
                    any(Runnable.class)
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
                            synchronized (capturedTraceIds) {
                                capturedTraceIds.add(tracer.currentSpan().context().traceId());
                            }

                            RoomJoinEvent event = new RoomJoinEvent(joinCode, playerName);
                            streamPublisher.publish(RoomStreamKey.BROADCAST, event);
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

            // then — 각 발행이 서로 다른 traceId를 가짐
            assertThat(capturedTraceIds)
                    .hasSize(playerNames.length)
                    .allSatisfy(traceId -> assertThat(traceId).isNotBlank())
                    .doesNotHaveDuplicates();

            // then — 모든 이벤트가 Consumer에서 정상 처리됨
            for (String playerName : playerNames) {
                awaitPlayerJoined(playerName);
            }

            // then — 각 traceId가 Consumer 측 캐리어로 전파됨
            for (String traceId : capturedTraceIds) {
                verify(streamTracePropagator).runInConsumerScope(
                        argThat(carrier -> {
                            String traceparent = carrier.get(StreamRecordFields.TRACEPARENT);
                            return traceparent != null && traceparent.contains(traceId);
                        }),
                        eq("RoomJoinEvent"),
                        any(Runnable.class)
                );
            }
        }
    }

    @Nested
    class Observation_스코프_바깥에서_이벤트_발행_시 {

        @Test
        void 레코드에_traceparent_필드가_실리지_않는다() {
            // given — 스코프 밖이므로 활성 스팬이 없다
            RoomJoinEvent event = new RoomJoinEvent(joinCode, "노트레이스");

            // when
            streamPublisher.publish(RoomStreamKey.BROADCAST, event);

            // then
            String traceparent = findPublishedTraceparent("노트레이스");
            assertThat(traceparent).isNull();
        }

        @Test
        void traceparent가_없어도_Consumer에서_이벤트는_정상_처리된다() {
            // given
            RoomJoinEvent event = new RoomJoinEvent(joinCode, "노트레이스처리");

            streamPublisher.publish(RoomStreamKey.BROADCAST, event);

            // then — trace 없어도 비즈니스 로직은 돌아야 한다
            awaitPlayerJoined("노트레이스처리");

            // then — 캐리어에 traceparent가 없는 상태로 스코프 실행이 호출되어야 하고,
            // 내부에서 traceparent 부재 시 스팬 생성 없이 task만 실행
            verify(streamTracePropagator).runInConsumerScope(
                    argThat(carrier -> !carrier.containsKey(StreamRecordFields.TRACEPARENT)),
                    eq("RoomJoinEvent"),
                    any(Runnable.class)
            );
        }
    }

    @Nested
    class 구형_레코드_포맷_호환 {

        @Test
        void 구형_ObjectRecord_포맷_메시지도_정상_처리된다() throws Exception {
            // given — 전환 이전 발행 코드가 사용하던 ObjectRecord(_raw 필드) 포맷 재현
            RoomJoinEvent event = new RoomJoinEvent(joinCode, "레거시폴백");
            String legacyPayload = redisObjectMapper.writeValueAsString(event);

            // when
            stringRedisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .in(RoomStreamKey.BROADCAST.getRedisKey())
                            .ofObject(legacyPayload)
            );

            // then — 신형 리스너가 _raw 폴백으로 페이로드를 읽어 정상 처리한다
            awaitPlayerJoined("레거시폴백");
        }

        @Test
        void 구형_traceInfo_필드가_남은_페이로드도_정상_처리된다() {
            // given — 구버전이 발행한 메시지에는 traceInfo 필드가 남아 있다
            String legacyPayload = """
                    {"@type":"RoomJoinEvent","eventId":"legacy-trace-info","traceInfo":{"traceId":"abc","spanId":"def"},\
                    "timestamp":"2026-01-01T00:00:00Z","joinCode":"%s","guestName":"구형필드","userId":null}""".formatted(joinCode);

            // when
            streamPublisher.publish(RoomStreamKey.BROADCAST.getRedisKey(), legacyPayload, null);

            // then — FAIL_ON_UNKNOWN_PROPERTIES=false라 미지 필드는 무시되고 정상 처리된다
            awaitPlayerJoined("구형필드");
        }
    }
}
