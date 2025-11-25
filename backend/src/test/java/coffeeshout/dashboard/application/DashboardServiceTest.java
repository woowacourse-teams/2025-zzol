package coffeeshout.dashboard.application;


import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.global.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.room.infra.persistence.RouletteResultEntity;
import coffeeshout.room.infra.persistence.RouletteResultJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DashboardServiceTest extends ServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private RouletteResultJpaRepository rouletteResultJpaRepository;

    @Autowired
    private MiniGameJpaRepository miniGameJpaRepository;

    @Nested
    @DisplayName("getTop5Winners 테스트")
    class GetTop5WinnersTest {

        @Test
        void 이번달_가장_많이_당첨된_닉네임_상위_5개를_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));

            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room, "철수", PlayerType.HOST)
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room, "영희", PlayerType.GUEST)
            );
            final PlayerEntity player3 = playerJpaRepository.save(
                    new PlayerEntity(room, "민수", PlayerType.GUEST)
            );

            // 철수 5번 당첨
            for (int i = 0; i < 5; i++) {
                rouletteResultJpaRepository.save(new RouletteResultEntity(room, player1, 50));
            }

            // 영희 3번 당첨
            for (int i = 0; i < 3; i++) {
                rouletteResultJpaRepository.save(new RouletteResultEntity(room, player2, 30));
            }

            // 민수 2번 당첨
            for (int i = 0; i < 2; i++) {
                rouletteResultJpaRepository.save(new RouletteResultEntity(room, player3, 20));
            }

            // when
            final List<TopWinnerResponse> result = dashboardService.getTop5Winners();

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).playerName()).isEqualTo("철수");
            assertThat(result.get(0).winCount()).isEqualTo(5);
            assertThat(result.get(1).playerName()).isEqualTo("영희");
            assertThat(result.get(1).winCount()).isEqualTo(3);
            assertThat(result.get(2).playerName()).isEqualTo("민수");
            assertThat(result.get(2).winCount()).isEqualTo(2);
        }

        @Test
        void 이번달_당첨_기록이_없으면_빈_리스트를_반환한다() {
            // when
            final List<TopWinnerResponse> result = dashboardService.getTop5Winners();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 같은_닉네임의_당첨은_합산된다() {
            // given
            final RoomEntity room1 = roomJpaRepository.save(new RoomEntity("AAAA"));
            final RoomEntity room2 = roomJpaRepository.save(new RoomEntity("BBBB"));

            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room1, "철수", PlayerType.HOST)
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room2, "철수", PlayerType.HOST)
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room1, player1, 50));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room2, player2, 30));

            // when
            final List<TopWinnerResponse> result = dashboardService.getTop5Winners();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).playerName()).isEqualTo("철수");
            assertThat(result.get(0).winCount()).isEqualTo(2);
        }

        @Test
        void 다섯개_이상이면_상위_5개만_반환한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("CCCC"));

            for (int i = 1; i <= 10; i++) {
                final PlayerEntity player = playerJpaRepository.save(
                        new PlayerEntity(room, "플레이어" + i, PlayerType.GUEST)
                );

                for (int j = 0; j < i; j++) {
                    rouletteResultJpaRepository.save(new RouletteResultEntity(room, player, 10));
                }
            }

            // when
            final List<TopWinnerResponse> result = dashboardService.getTop5Winners();

            // then
            assertThat(result).hasSize(5);
            assertThat(result.get(0).playerName()).isEqualTo("플레이어10");
            assertThat(result.get(0).winCount()).isEqualTo(10);
            assertThat(result.get(4).playerName()).isEqualTo("플레이어6");
            assertThat(result.get(4).winCount()).isEqualTo(6);
        }


    }

    @Nested
    @DisplayName("getLowestProbabilityWinner 테스트")
    class GetLowestProbabilityWinnerTest {

        @Test
        void 이번달_최소_확률로_당첨된_닉네임을_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("DDDD"));

            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room, "철수", PlayerType.HOST)
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room, "영희", PlayerType.GUEST)
            );
            final PlayerEntity player3 = playerJpaRepository.save(
                    new PlayerEntity(room, "민수", PlayerType.GUEST)
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player1, 50));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player2, 30));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player3, 5));

            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            assertThat(result.probability()).isEqualTo(0.05);
            assertThat(result.playerNames()).containsExactly("민수");
        }

        @Test
        void 같은_최소_확률로_당첨된_사람이_여러명이면_모두_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("GGGG"));

            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room, "철수", PlayerType.HOST)
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room, "영희", PlayerType.GUEST)
            );
            final PlayerEntity player3 = playerJpaRepository.save(
                    new PlayerEntity(room, "민수", PlayerType.GUEST)
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player1, 50));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player2, 3));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player3, 3));

            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            assertThat(result.probability()).isEqualTo(0.03);
            assertThat(result.playerNames()).containsExactlyInAnyOrder("영희", "민수");
        }

        @Test
        void 최소_확률_당첨자가_5명_이상이면_5명만_반환한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("HHHH"));

            for (int i = 1; i <= 10; i++) {
                final PlayerEntity player = playerJpaRepository.save(
                        new PlayerEntity(room, "플레이어" + i, PlayerType.GUEST)
                );
                rouletteResultJpaRepository.save(new RouletteResultEntity(room, player, 1));
            }

            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            assertThat(result.probability()).isEqualTo(0.01);
            assertThat(result.playerNames()).hasSize(5);
        }

        @Test
        void 여러_방의_당첨_기록_중_최소_확률을_조회한다() {
            // given
            final RoomEntity room1 = roomJpaRepository.save(new RoomEntity("EEEE"));
            final RoomEntity room2 = roomJpaRepository.save(new RoomEntity("FFFF"));

            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room1, "철수", PlayerType.HOST)
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room2, "영희", PlayerType.HOST)
            );
            final PlayerEntity player3 = playerJpaRepository.save(
                    new PlayerEntity(room2, "민수", PlayerType.GUEST)
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room1, player1, 20));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room2, player2, 10));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room2, player3, 3));

            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            assertThat(result.probability()).isEqualTo(0.03);
            assertThat(result.playerNames()).containsExactly("민수");
        }

        @Test
        void 이번달_당첨_기록이_없으면_null을_반환한다() {
            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getGamePlayCounts 테스트")
            // TODO MiniGameType 추가되면 테스트들에도 추가해야함
    class GetGamePlayCountsTest {

        @Test
        void 이번달_가장_많이_실행된_게임_순으로_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("IIII"));

            miniGameJpaRepository.save(new MiniGameEntity(room, MiniGameType.CARD_GAME));
            miniGameJpaRepository.save(new MiniGameEntity(room, MiniGameType.CARD_GAME));
            miniGameJpaRepository.save(new MiniGameEntity(room, MiniGameType.CARD_GAME));

            // when
            final List<GamePlayCountResponse> result = dashboardService.getGamePlayCounts();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).gameType()).isEqualTo(MiniGameType.CARD_GAME);
            assertThat(result.get(0).playCount()).isEqualTo(3);
        }

        @Test
        void 여러_게임을_실행_횟수_순으로_정렬한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("JJJJ"));

            // CARD_GAME 5회
            for (int i = 0; i < 5; i++) {
                miniGameJpaRepository.save(new MiniGameEntity(room, MiniGameType.CARD_GAME));
            }

            // when
            final List<GamePlayCountResponse> result = dashboardService.getGamePlayCounts();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).gameType()).isEqualTo(MiniGameType.CARD_GAME);
            assertThat(result.get(0).playCount()).isEqualTo(5);
        }

        @Test
        void 여러_방의_게임_실행_횟수를_합산한다() {
            // given
            final RoomEntity room1 = roomJpaRepository.save(new RoomEntity("KKKK"));
            final RoomEntity room2 = roomJpaRepository.save(new RoomEntity("LLLL"));

            miniGameJpaRepository.save(new MiniGameEntity(room1, MiniGameType.CARD_GAME));
            miniGameJpaRepository.save(new MiniGameEntity(room1, MiniGameType.CARD_GAME));
            miniGameJpaRepository.save(new MiniGameEntity(room2, MiniGameType.CARD_GAME));

            // when
            final List<GamePlayCountResponse> result = dashboardService.getGamePlayCounts();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).gameType()).isEqualTo(MiniGameType.CARD_GAME);
            assertThat(result.get(0).playCount()).isEqualTo(3);
        }

        @Test
        void 이번달_게임_실행_기록이_없으면_빈_리스트를_반환한다() {
            // when
            final List<GamePlayCountResponse> result = dashboardService.getGamePlayCounts();

            // then
            assertThat(result).isEmpty();
        }
    }
}
