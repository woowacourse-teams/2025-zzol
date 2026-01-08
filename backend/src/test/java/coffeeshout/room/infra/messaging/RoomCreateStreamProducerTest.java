package coffeeshout.room.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.support.test.IntegrationTest;
import java.time.Duration;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class RoomCreateStreamProducerTest {

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    StreamPublisher streamPublisher;

    @Autowired
    JoinCodeGenerator joinCodeGenerator;

    @Nested
    class 실제_비즈니스_로직_테스트 {

        @Test
        void 방_생성_처리_시_실제_Room_엔티티가_올바르게_생성된다() {
            // given
            String hostName = "호스트";
            String joinCode = joinCodeGenerator.generate().getValue();

            RoomCreateEvent event = new RoomCreateEvent(hostName, joinCode);

            // when
            streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);

            // then
            await().atMost(Duration.ofSeconds(3)).pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Room createdRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow(
                                () -> new IllegalStateException("방이 생성되지 않음")
                        );

                        SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(createdRoom.getJoinCode().getValue()).isEqualTo(joinCode);
                            softly.assertThat(createdRoom.getPlayers()).hasSize(1);
                            softly.assertThat(createdRoom.getPlayers().getFirst().getName().value())
                                    .isEqualTo(hostName);
                        });
                    });
        }

        @Test
        void 여러_방이_동시에_생성되어도_모두_올바르게_처리된다() {
            // given
            String[][] roomData = {
                    {"호스트1", joinCodeGenerator.generate().getValue()},
                    {"호스트2", joinCodeGenerator.generate().getValue()},
                    {"호스트3", joinCodeGenerator.generate().getValue()}
            };

            // when
            for (String[] data : roomData) {
                RoomCreateEvent event = new RoomCreateEvent(data[0], data[1]);
                streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
            }

            // then
            for (String[] data : roomData) {
                await().atMost(Duration.ofSeconds(3)).pollInterval(Duration.ofMillis(100))
                        .untilAsserted(() -> {
                            Room createdRoom = roomRepository.findByJoinCode(new JoinCode(data[1])).orElseThrow(
                                    () -> new IllegalStateException("방이 생성되지 않음: " + data[1])
                            );

                            assertThat(createdRoom.getPlayers().getFirst().getName().value()).isEqualTo(data[0]);
                        });
            }
        }

        @Test
        void 동일_joinCode로_방_생성_이벤트가_중복_발행되어도_방은_한_번만_생성된다() {
            // given
            String hostName = "중복테스트호스트";
            String joinCode = joinCodeGenerator.generate().getValue();

            RoomCreateEvent event = new RoomCreateEvent(hostName, joinCode);

            // when
            streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
            streamPublisher.publish(StreamKey.ROOM_BROADCAST, event); // 중복 발행

            // then
            await().atMost(Duration.ofSeconds(3)).pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Room createdRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow(
                                () -> new IllegalStateException("방이 생성되지 않음")
                        );

                        assertThat(createdRoom.getPlayers()).hasSize(1); // 호스트 한 명만 존재
                    });
        }
    }
}
