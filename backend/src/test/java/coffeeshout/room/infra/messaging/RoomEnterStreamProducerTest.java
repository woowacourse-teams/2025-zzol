package coffeeshout.room.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.support.test.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class RoomEnterStreamProducerTest {

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    RoomEnterStreamProducer producer;

    private String joinCode;

    @BeforeEach
    void setUp() {
        final Room testRoom = RoomFixture.호스트_꾹이();
        roomRepository.save(testRoom);
        joinCode = testRoom.getJoinCode().getValue();
    }

    @Nested
    class 실제_비즈니스_로직_테스트 {

        @Test
        void 플레이어_입장_처리_시_실제_Room_엔티티가_올바르게_업데이트된다() {
            // given
            String playerName = "인원 추가";

            producer.broadcastEnterRoom(new RoomJoinEvent(joinCode, playerName));

            // then
            await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Room updatedRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow();
                        Player result = updatedRoom.getPlayers().stream()
                                .filter(player -> playerName.equals(player.getName().value())).findFirst()
                                .orElseThrow(() -> new IllegalStateException("플레이어가 추가되지 않음"));

                        assertThat(result.getName().value()).isEqualTo(playerName);
                    });
        }

        @Test
        void 여러_플레이어가_동시에_입장해도_모두_올바르게_처리된다() {
            // given
            String[] playerNames = {"플레이어1", "플레이어2", "플레이어3"};

            for (int i = 0; i < playerNames.length; i++) {
                RoomJoinEvent roomJoinEvent = new RoomJoinEvent(joinCode, playerNames[i]);
                producer.broadcastEnterRoom(roomJoinEvent);
            }

            // then
            await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Room updatedRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow();
                        assertThat(updatedRoom.getPlayers())
                                .extracting(player -> player.getName().value())
                                .contains(playerNames);
                    });
        }

        @Test
        void JSON_직렬화_방식으로_메시지_전송이_정상_처리된다() {
            // given
            String playerName = "JSON테스트";

            // when
            producer.broadcastEnterRoom(new RoomJoinEvent(joinCode, playerName));

            // then
            await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Room updatedRoom = roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow();
                        boolean playerExists = updatedRoom.getPlayers().stream()
                                .anyMatch(player -> playerName.equals(player.getName().value()));

                        assertThat(playerExists).isTrue();
                    });
        }
    }
}
