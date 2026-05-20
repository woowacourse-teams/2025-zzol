package coffeeshout.cardgame.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import coffeeshout.fixture.IntegrationTestSupport;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.gamecommon.infra.GameStreamKey;
import coffeeshout.redis.stream.StreamPublisher;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

class CardSelectStreamProducerTest extends IntegrationTestSupport {

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    StreamPublisher streamPublisher;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private JoinCode joinCode;
    private String cardGameStreamKey;

    @BeforeEach
    void setUp() {
        Room room = RoomFixture.호스트_꾹이();
        roomRepository.save(room);
        joinCode = room.getJoinCode();
        cardGameStreamKey = GameStreamKey.CARDGAME_SELECT_BROADCAST.getRedisKey();
    }

    @Nested
    class 카드선택_브로드캐스트_테스트 {

        @Test
        void 카드선택_이벤트를_성공적으로_브로드캐스트한다() {
            // given
            String playerName = "꾹이";
            Integer cardIndex = 0;
            SelectCardCommandEvent event = new SelectCardCommandEvent(
                    joinCode.getValue(), playerName, cardIndex);

            // when
            streamPublisher.publish(GameStreamKey.CARDGAME_SELECT_BROADCAST, event);

            // then
            await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Long streamSize = redisTemplate.opsForStream().size(cardGameStreamKey);
                        assertThat(streamSize).isGreaterThan(0);
                    });
        }

        @Test
        void 여러_플레이어가_카드를_선택해도_모두_올바르게_처리된다() {
            // given
            String[] playerNames = {"꾹이", "루키", "엠제이"};
            Integer[] cardIndexes = {0, 1, 2};

            // when
            for (int i = 0; i < playerNames.length; i++) {
                SelectCardCommandEvent event = new SelectCardCommandEvent(
                        joinCode.getValue(), playerNames[i], cardIndexes[i]);
                streamPublisher.publish(GameStreamKey.CARDGAME_SELECT_BROADCAST, event);
            }

            // then
            await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        Long streamSize = redisTemplate.opsForStream().size(cardGameStreamKey);
                        assertThat(streamSize).isGreaterThanOrEqualTo(3);
                    });
        }
    }
}
