package coffeeshout.minigame.cardgame.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameRandomDeckGenerator;
import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import coffeeshout.cardgame.infra.messaging.CardSelectStreamProducer;
import coffeeshout.fixture.CardGameFake;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.global.config.properties.RedisStreamProperties;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.support.test.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

@IntegrationTest
class CardSelectStreamProducerTest {

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    CardSelectStreamProducer cardSelectStreamProducer;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    RedisStreamProperties redisStreamProperties;

    private JoinCode joinCode;
    private String cardGameStreamKey;

    @BeforeEach
    void setUp() {
        Room room = RoomFixture.호스트_꾹이();
        Player host = room.getHost();

        // 카드 게임을 start 상태로 전환한다.
        CardGame cardGame = new CardGameFake(new CardGameRandomDeckGenerator());
        cardGame.startPlay();

        room.addMiniGame(host.getName(), cardGame);

                // 모든 플레이어를 Ready 상태로 전환 후 시작한다.
        for (final Player player : room.getPlayers()) {
            player.updateReadyState(true);
        }
        room.startNextGame(host.getName().value());

        roomRepository.save(room);
        joinCode = room.getJoinCode();

        cardGameStreamKey = redisStreamProperties.cardGameSelectKey();

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
            cardSelectStreamProducer.broadcastCardSelect(event);

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
                cardSelectStreamProducer.broadcastCardSelect(event);
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
