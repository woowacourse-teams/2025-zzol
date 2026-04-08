package coffeeshout.minigame.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.fixture.PlayerFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.repository.MiniGameResultSavePersistence;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MiniGameResultSaveJpaPersistenceTest extends ServiceTest {

    @Autowired
    private MiniGameResultSavePersistence miniGameResultSavePersistence;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private MiniGameJpaRepository miniGameJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 테스트용 DB 상태 구성: RoomEntity + PlayerEntity들 + MiniGameEntity
     */
    private SetupResult setUp(String joinCode, MiniGameType gameType, String... playerNames) {
        final RoomEntity room = roomJpaRepository.save(new RoomEntity(joinCode));
        final MiniGameEntity miniGame = miniGameJpaRepository.save(new MiniGameEntity(room, gameType));

        for (int i = 0; i < playerNames.length; i++) {
            final PlayerType type = (i == 0) ? PlayerType.HOST : PlayerType.GUEST;
            playerJpaRepository.save(new PlayerEntity(room, playerNames[i], type));
        }
        return new SetupResult(room, miniGame);
    }

    record SetupResult(RoomEntity room, MiniGameEntity miniGame) {}

    @Nested
    class 미니게임_결과_저장 {

        @Test
        void 플레이어_수만큼_결과가_저장된다() {
            // given
            setUp("ABCD", MiniGameType.CARD_GAME, "한스", "꾹이", "루키");

            final Player 한스 = PlayerFixture.호스트한스();
            final Player 꾹이 = PlayerFixture.게스트꾹이();
            final Player 루키 = PlayerFixture.게스트루키();
            final List<Player> players = List.of(한스, 꾹이, 루키);

            final MiniGameResult result = new MiniGameResult(Map.of(한스, 1, 꾹이, 2, 루키, 3));
            final Map<Player, MiniGameScore> scores = Map.of(
                    한스, new CardGameScore(300),
                    꾹이, new CardGameScore(200),
                    루키, new CardGameScore(100)
            );

            // when
            miniGameResultSavePersistence.saveResults("ABCD", MiniGameType.CARD_GAME, players, result, scores);

            // then - bulkInsert는 JDBC 직접 저장이므로 flush 없이 바로 조회 가능
            final int savedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM mini_game_result",
                    Integer.class
            );
            assertThat(savedCount).isEqualTo(3);
        }

        @Test
        void 저장된_결과의_rank와_score와_miniGameType이_올바르다() {
            // given
            setUp("ABCD", MiniGameType.CARD_GAME, "한스", "꾹이");

            final Player 한스 = PlayerFixture.호스트한스();
            final Player 꾹이 = PlayerFixture.게스트꾹이();

            final MiniGameResult result = new MiniGameResult(Map.of(한스, 1, 꾹이, 2));
            final Map<Player, MiniGameScore> scores = Map.of(
                    한스, new CardGameScore(500),
                    꾹이, new CardGameScore(300)
            );

            // when
            miniGameResultSavePersistence.saveResults("ABCD", MiniGameType.CARD_GAME, List.of(한스, 꾹이), result, scores);

            // then - 한스(rank=1, score=500), 꾹이(rank=2, score=300) 검증
            final List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT mgr.player_rank, mgr.score, mgr.mini_game_type, p.player_name "
                    + "FROM mini_game_result mgr "
                    + "JOIN player p ON mgr.player_id = p.id "
                    + "ORDER BY mgr.player_rank"
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(rows).hasSize(2);

                final Map<String, Object> 한스결과 = rows.get(0);
                softly.assertThat(한스결과.get("player_name")).isEqualTo("한스");
                softly.assertThat(한스결과.get("player_rank")).isEqualTo(1);
                softly.assertThat(한스결과.get("score")).isEqualTo(500L);
                softly.assertThat(한스결과.get("mini_game_type")).isEqualTo("CARD_GAME");

                final Map<String, Object> 꾹이결과 = rows.get(1);
                softly.assertThat(꾹이결과.get("player_name")).isEqualTo("꾹이");
                softly.assertThat(꾹이결과.get("player_rank")).isEqualTo(2);
                softly.assertThat(꾹이결과.get("score")).isEqualTo(300L);
            });
        }

        @Test
        void DB에_없는_플레이어_이름이_포함되면_예외를_발생시킨다() {
            // given
            setUp("ABCD", MiniGameType.CARD_GAME, "한스");

            final Player 한스 = PlayerFixture.호스트한스();
            final Player 존재하지않는플레이어 = PlayerFixture.게스트꾹이(); // DB에 없음

            final MiniGameResult result = new MiniGameResult(Map.of(한스, 1, 존재하지않는플레이어, 2));
            final Map<Player, MiniGameScore> scores = Map.of(
                    한스, new CardGameScore(100),
                    존재하지않는플레이어, new CardGameScore(50)
            );

            // when & then
            assertThatThrownBy(() ->
                    miniGameResultSavePersistence.saveResults(
                            "ABCD", MiniGameType.CARD_GAME,
                            List.of(한스, 존재하지않는플레이어),
                            result, scores
                    )
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("플레이어가 존재하지 않습니다");
        }
    }
}
