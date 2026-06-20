package coffeeshout.dashboard.application;


import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.blindtimer.domain.BlindTimerScore;
import coffeeshout.dashboard.domain.BlindTimerTopPlayerResponse;
import coffeeshout.dashboard.domain.BlockStackingTopPlayerResponse;
import coffeeshout.dashboard.domain.GamePlayCountResponse;
import coffeeshout.dashboard.domain.LowestProbabilityWinnerResponse;
import coffeeshout.dashboard.domain.SpeedTouchTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.AdminModuleServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultEntity;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.room.infra.persistence.RouletteResultEntity;
import coffeeshout.room.infra.persistence.RouletteResultJpaRepository;
import coffeeshout.speedtouch.domain.SpeedTouchScore;
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DashboardServiceTest extends AdminModuleServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private RouletteResultJpaRepository rouletteResultJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private MiniGameJpaRepository miniGameJpaRepository;

    @Autowired
    private MiniGameResultJpaRepository miniGameResultJpaRepository;

    @Nested
    @DisplayName("getTop5Winners 테스트")
    class GetTop5WinnersTest {

        @Test
        void 이번달_가장_많이_당첨된_로그인_사용자_상위_3명을_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));

            final UserEntity user1 = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final UserEntity user2 = userJpaRepository.save(new UserEntity("XY4ZQ", "영희"));
            final UserEntity user3 = userJpaRepository.save(new UserEntity("GH7KL", "민수"));

            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room, "철수", PlayerType.HOST, user1.getId())
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room, "영희", PlayerType.GUEST, user2.getId())
            );
            final PlayerEntity player3 = playerJpaRepository.save(
                    new PlayerEntity(room, "민수", PlayerType.GUEST, user3.getId())
            );

            for (int i = 0; i < 5; i++) {
                rouletteResultJpaRepository.save(new RouletteResultEntity(room, player1, 50));
            }
            for (int i = 0; i < 3; i++) {
                rouletteResultJpaRepository.save(new RouletteResultEntity(room, player2, 30));
            }
            for (int i = 0; i < 2; i++) {
                rouletteResultJpaRepository.save(new RouletteResultEntity(room, player3, 20));
            }

            // when
            final List<TopWinnerResponse> result = dashboardService.getTop5Winners();

            // then
            assertThat(result).hasSize(3);
            assertThat(result.getFirst().nickname()).isEqualTo("철수");
            assertThat(result.getFirst().winCount()).isEqualTo(5);
            assertThat(result.get(1).nickname()).isEqualTo("영희");
            assertThat(result.get(1).winCount()).isEqualTo(3);
            assertThat(result.get(2).nickname()).isEqualTo("민수");
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
        void 로그인하지_않은_게스트의_당첨은_집계에서_제외된다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("AAAA"));

            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final PlayerEntity loginPlayer = playerJpaRepository.save(
                    new PlayerEntity(room, "철수", PlayerType.HOST, user.getId())
            );
            final PlayerEntity guestPlayer = playerJpaRepository.save(
                    new PlayerEntity(room, "게스트", PlayerType.GUEST)
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room, loginPlayer, 50));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, guestPlayer, 30));

            // when
            final List<TopWinnerResponse> result = dashboardService.getTop5Winners();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().nickname()).isEqualTo("철수");
        }

        @Test
        void 같은_userId의_당첨은_여러_방에서도_합산된다() {
            // given
            final RoomEntity room1 = roomJpaRepository.save(new RoomEntity("BBBB"));
            final RoomEntity room2 = roomJpaRepository.save(new RoomEntity("CCCC"));

            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room1, "철수", PlayerType.HOST, user.getId())
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room2, "철수", PlayerType.HOST, user.getId())
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room1, player1, 50));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room2, player2, 30));

            // when
            final List<TopWinnerResponse> result = dashboardService.getTop5Winners();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().winCount()).isEqualTo(2);
        }

        @Test
        void 다섯개_이상이면_상위_5개만_반환한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("DDDD"));

            for (int i = 1; i <= 10; i++) {
                final UserEntity user = userJpaRepository.save(
                        new UserEntity("AB" + "CDFGHJKLMN".charAt(i - 1) + "3D", "플레이어" + i)
                );
                final PlayerEntity player = playerJpaRepository.save(
                        new PlayerEntity(room, "플레이어" + i, PlayerType.GUEST, user.getId())
                );
                for (int j = 0; j < i; j++) {
                    rouletteResultJpaRepository.save(new RouletteResultEntity(room, player, 10));
                }
            }

            // when
            final List<TopWinnerResponse> result = dashboardService.getTop5Winners();

            // then
            assertThat(result).hasSize(5);
            assertThat(result.getFirst().nickname()).isEqualTo("플레이어10");
            assertThat(result.getFirst().winCount()).isEqualTo(10);
            assertThat(result.get(4).nickname()).isEqualTo("플레이어6");
            assertThat(result.get(4).winCount()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("getLowestProbabilityWinner 테스트")
    class GetLowestProbabilityWinnerTest {

        @Test
        void 이번달_최소_확률로_당첨된_로그인_사용자_닉네임과_유저코드를_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("DDDD"));

            final UserEntity user1 = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final UserEntity user2 = userJpaRepository.save(new UserEntity("XY4ZQ", "영희"));
            final UserEntity user3 = userJpaRepository.save(new UserEntity("GH7KL", "민수"));

            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room, "철수", PlayerType.HOST, user1.getId())
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room, "영희", PlayerType.GUEST, user2.getId())
            );
            final PlayerEntity player3 = playerJpaRepository.save(
                    new PlayerEntity(room, "민수", PlayerType.GUEST, user3.getId())
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player1, 50));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player2, 30));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player3, 5));

            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.probability()).isEqualTo(0.05);
                softly.assertThat(result.players()).hasSize(1);
                softly.assertThat(result.players().getFirst().nickname()).isEqualTo("민수");
                softly.assertThat(result.players().getFirst().userCode()).isEqualTo("GH7KL");
            });
        }

        @Test
        void 같은_최소_확률로_당첨된_로그인_사용자가_여러명이면_모두_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("GGGG"));

            final UserEntity user1 = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final UserEntity user2 = userJpaRepository.save(new UserEntity("XY4ZQ", "영희"));
            final UserEntity user3 = userJpaRepository.save(new UserEntity("GH7KL", "민수"));

            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room, "철수", PlayerType.HOST, user1.getId())
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room, "영희", PlayerType.GUEST, user2.getId())
            );
            final PlayerEntity player3 = playerJpaRepository.save(
                    new PlayerEntity(room, "민수", PlayerType.GUEST, user3.getId())
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player1, 50));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player2, 3));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, player3, 3));

            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            assertThat(result.probability()).isEqualTo(0.03);
            assertThat(result.players())
                    .extracting(LowestProbabilityWinnerResponse.PlayerInfo::nickname)
                    .containsExactlyInAnyOrder("영희", "민수");
        }

        @Test
        void 최소_확률_당첨자가_5명_이상이면_5명만_반환한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("HHHH"));
            final String[] codes = {"AB3C", "AB4C", "AB6C", "AB7C", "AB8C", "AB9C", "AC3D", "AC4D", "AC6D", "AC7D"};

            for (int i = 0; i < 10; i++) {
                final UserEntity user = userJpaRepository.save(new UserEntity(codes[i], "플레이어" + (i + 1)));
                final PlayerEntity player = playerJpaRepository.save(
                        new PlayerEntity(room, "플레이어" + (i + 1), PlayerType.GUEST, user.getId())
                );
                rouletteResultJpaRepository.save(new RouletteResultEntity(room, player, 1));
            }

            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            assertThat(result.probability()).isEqualTo(0.01);
            assertThat(result.players()).hasSize(5);
        }

        @Test
        void 여러_방의_당첨_기록_중_최소_확률을_조회한다() {
            // given
            final RoomEntity room1 = roomJpaRepository.save(new RoomEntity("EEEE"));
            final RoomEntity room2 = roomJpaRepository.save(new RoomEntity("FFFF"));

            final UserEntity user1 = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final UserEntity user2 = userJpaRepository.save(new UserEntity("XY4ZQ", "영희"));
            final UserEntity user3 = userJpaRepository.save(new UserEntity("GH7KL", "민수"));

            final PlayerEntity player1 = playerJpaRepository.save(
                    new PlayerEntity(room1, "철수", PlayerType.HOST, user1.getId())
            );
            final PlayerEntity player2 = playerJpaRepository.save(
                    new PlayerEntity(room2, "영희", PlayerType.HOST, user2.getId())
            );
            final PlayerEntity player3 = playerJpaRepository.save(
                    new PlayerEntity(room2, "민수", PlayerType.GUEST, user3.getId())
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room1, player1, 20));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room2, player2, 10));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room2, player3, 3));

            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            assertThat(result.probability()).isEqualTo(0.03);
            assertThat(result.players())
                    .extracting(LowestProbabilityWinnerResponse.PlayerInfo::nickname)
                    .containsExactly("민수");
        }

        @Test
        void 게스트_플레이어의_당첨은_최소_확률_집계에서_제외된다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("PPQQ"));

            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final PlayerEntity loginPlayer = playerJpaRepository.save(
                    new PlayerEntity(room, "철수", PlayerType.HOST, user.getId())
            );
            final PlayerEntity guestPlayer = playerJpaRepository.save(
                    new PlayerEntity(room, "게스트", PlayerType.GUEST)
            );

            rouletteResultJpaRepository.save(new RouletteResultEntity(room, loginPlayer, 30));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, guestPlayer, 1));

            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then: 게스트(확률 1)는 제외되고, 로그인 사용자(확률 30)만 집계됨
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.probability()).isEqualTo(0.30);
                softly.assertThat(result.players())
                        .extracting(LowestProbabilityWinnerResponse.PlayerInfo::nickname)
                        .containsExactly("철수");
            });
        }

        @Test
        void 이번달_당첨_기록이_없으면_빈_구조체를_반환한다() {
            // when
            final LowestProbabilityWinnerResponse result = dashboardService.getLowestProbabilityWinner();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.probability()).isEqualTo(0.0);
                softly.assertThat(result.players()).isEmpty();
            });
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
            assertThat(result.getFirst().gameType()).isEqualTo(MiniGameType.CARD_GAME);
            assertThat(result.getFirst().playCount()).isEqualTo(3);
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
            assertThat(result.getFirst().gameType()).isEqualTo(MiniGameType.CARD_GAME);
            assertThat(result.getFirst().playCount()).isEqualTo(5);
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
            assertThat(result.getFirst().gameType()).isEqualTo(MiniGameType.CARD_GAME);
            assertThat(result.getFirst().playCount()).isEqualTo(3);
        }

        @Test
        void 이번달_게임_실행_기록이_없으면_빈_리스트를_반환한다() {
            // when
            final List<GamePlayCountResponse> result = dashboardService.getGamePlayCounts();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBlockStackingTopPlayers 테스트")
    class GetBlockStackingTopPlayersTest {

        @Test
        void 이번달_블록쌓기_최고_층수_기준_상위_5명을_내림차순으로_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("MMNN"));
            final MiniGameEntity miniGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.BLOCK_STACKING)
            );

            final PlayerEntity 철수 = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));
            final PlayerEntity 영희 = playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST));
            final PlayerEntity 민수 = playerJpaRepository.save(new PlayerEntity(room, "민수", PlayerType.GUEST));

            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 철수, 1, 30L));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 영희, 2, 20L));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 민수, 3, 10L));

            // when
            final List<BlockStackingTopPlayerResponse> result = dashboardService.getBlockStackingTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(3);
                softly.assertThat(result.get(0).playerName()).isEqualTo("철수");
                softly.assertThat(result.get(0).maxFloor()).isEqualTo(30L);
                softly.assertThat(result.get(1).playerName()).isEqualTo("영희");
                softly.assertThat(result.get(2).playerName()).isEqualTo("민수");
            });
        }

        @Test
        void 이번달_블록쌓기_기록이_없으면_빈_리스트를_반환한다() {
            // when
            final List<BlockStackingTopPlayerResponse> result = dashboardService.getBlockStackingTopPlayers();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 다섯명_초과이면_상위_5명만_반환한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("TTUV"));
            final MiniGameEntity miniGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.BLOCK_STACKING)
            );

            for (int i = 1; i <= 10; i++) {
                final PlayerEntity player = playerJpaRepository.save(
                        new PlayerEntity(room, "플레이어" + i, PlayerType.GUEST)
                );
                miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, player, i, (long) i * 10));
            }

            // when
            final List<BlockStackingTopPlayerResponse> result = dashboardService.getBlockStackingTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(5);
                softly.assertThat(result.getFirst().maxFloor()).isEqualTo(100L);
            });
        }

        @Test
        void 다른_게임_타입의_결과는_포함하지_않는다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("WWXX"));
            final MiniGameEntity blockStackingGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.BLOCK_STACKING)
            );
            final MiniGameEntity racingGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.RACING_GAME)
            );

            final PlayerEntity 철수 = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));
            final PlayerEntity 영희 = playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST));

            miniGameResultJpaRepository.save(new MiniGameResultEntity(blockStackingGame, 철수, 1, 20L));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(racingGame, 영희, 1, 5000L));

            // when
            final List<BlockStackingTopPlayerResponse> result = dashboardService.getBlockStackingTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.getFirst().playerName()).isEqualTo("철수");
            });
        }
    }

    @Nested
    @DisplayName("getSpeedTouchTopPlayers 테스트")
    class GetSpeedTouchTopPlayersTest {

        @Test
        void 이번달_스피드터치_최단_완주시간_기준_오름차순으로_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("STAA"));
            final MiniGameEntity miniGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.SPEED_TOUCH)
            );

            final PlayerEntity 철수 = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));
            final PlayerEntity 영희 = playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST));
            final PlayerEntity 민수 = playerJpaRepository.save(new PlayerEntity(room, "민수", PlayerType.GUEST));

            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 철수, 1, 1000L));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 영희, 2, 2000L));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 민수, 3, 3000L));

            // when
            final List<SpeedTouchTopPlayerResponse> result = dashboardService.getSpeedTouchTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(3);
                softly.assertThat(result.get(0).playerName()).isEqualTo("철수");
                softly.assertThat(result.get(0).bestTime()).isEqualTo(1000L);
                softly.assertThat(result.get(2).playerName()).isEqualTo("민수");
            });
        }

        @Test
        void 미완주_DNF_기록은_집계에서_제외된다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("STBB"));
            final MiniGameEntity miniGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.SPEED_TOUCH)
            );
            final PlayerEntity 완주자 = playerJpaRepository.save(new PlayerEntity(room, "완주자", PlayerType.HOST));
            final PlayerEntity 미완주자 = playerJpaRepository.save(new PlayerEntity(room, "미완주자", PlayerType.GUEST));

            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 완주자, 1, 4000L));
            // DNF 점수는 도메인 정의(SpeedTouchScore.ofDnf)에서 생성해 경계값이 실제 정의와 회귀로 묶이게 한다
            miniGameResultJpaRepository.save(
                    new MiniGameResultEntity(miniGame, 미완주자, 2, SpeedTouchScore.ofDnf(2).getValue()));

            // when
            final List<SpeedTouchTopPlayerResponse> result = dashboardService.getSpeedTouchTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.getFirst().playerName()).isEqualTo("완주자");
                softly.assertThat(result.getFirst().bestTime()).isEqualTo(4000L);
            });
        }

        @Test
        void 같은_플레이어의_여러_기록은_최단_시간으로_집계된다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("STCC"));
            final MiniGameEntity miniGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.SPEED_TOUCH)
            );
            final PlayerEntity 철수 = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));

            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 철수, 1, 5000L));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 철수, 1, 2000L));

            // when
            final List<SpeedTouchTopPlayerResponse> result = dashboardService.getSpeedTouchTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.getFirst().bestTime()).isEqualTo(2000L);
            });
        }

        @Test
        void 다섯명_초과이면_상위_5명만_반환한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("STDD"));
            final MiniGameEntity miniGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.SPEED_TOUCH)
            );

            for (int i = 1; i <= 7; i++) {
                final PlayerEntity player = playerJpaRepository.save(
                        new PlayerEntity(room, "플레이어" + i, PlayerType.GUEST)
                );
                miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, player, i, (long) i * 1000));
            }

            // when
            final List<SpeedTouchTopPlayerResponse> result = dashboardService.getSpeedTouchTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(5);
                softly.assertThat(result.getFirst().bestTime()).isEqualTo(1000L);
            });
        }

        @Test
        void 다른_게임_타입의_결과는_포함하지_않는다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("STEE"));
            final MiniGameEntity speedTouch = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.SPEED_TOUCH)
            );
            final MiniGameEntity racing = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.RACING_GAME)
            );

            final PlayerEntity 철수 = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));
            final PlayerEntity 영희 = playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST));

            miniGameResultJpaRepository.save(new MiniGameResultEntity(speedTouch, 철수, 1, 1500L));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(racing, 영희, 1, 500L));

            // when
            final List<SpeedTouchTopPlayerResponse> result = dashboardService.getSpeedTouchTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.getFirst().playerName()).isEqualTo("철수");
            });
        }

        @Test
        void 이번달_스피드터치_기록이_없으면_빈_리스트를_반환한다() {
            // when
            final List<SpeedTouchTopPlayerResponse> result = dashboardService.getSpeedTouchTopPlayers();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBlindTimerTopPlayers 테스트")
    class GetBlindTimerTopPlayersTest {

        @Test
        void 이번달_목표시간과의_오차가_작은_순으로_오름차순_조회한다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("BTAA"));
            final MiniGameEntity miniGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.BLIND_TIMER)
            );
            final PlayerEntity 철수 = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));
            final PlayerEntity 영희 = playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST));

            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 철수, 1, 120L));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 영희, 2, 800L));

            // when
            final List<BlindTimerTopPlayerResponse> result = dashboardService.getBlindTimerTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(2);
                softly.assertThat(result.get(0).playerName()).isEqualTo("철수");
                softly.assertThat(result.get(0).bestErrorMillis()).isEqualTo(120L);
                softly.assertThat(result.get(1).playerName()).isEqualTo("영희");
            });
        }

        @Test
        void 타임아웃_기록은_집계에서_제외된다() {
            // given
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("BTBB"));
            final MiniGameEntity miniGame = miniGameJpaRepository.save(
                    new MiniGameEntity(room, MiniGameType.BLIND_TIMER)
            );
            final PlayerEntity 정상 = playerJpaRepository.save(new PlayerEntity(room, "정상", PlayerType.HOST));
            final PlayerEntity 타임아웃 = playerJpaRepository.save(new PlayerEntity(room, "타임아웃", PlayerType.GUEST));

            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, 정상, 1, 3000L));
            // 타임아웃 점수는 도메인 정의(BlindTimerScore.ofTimeout)에서 생성해 경계값이 실제 정의와 회귀로 묶이게 한다
            miniGameResultJpaRepository.save(
                    new MiniGameResultEntity(miniGame, 타임아웃, 2, BlindTimerScore.ofTimeout().getValue()));

            // when
            final List<BlindTimerTopPlayerResponse> result = dashboardService.getBlindTimerTopPlayers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.getFirst().playerName()).isEqualTo("정상");
                softly.assertThat(result.getFirst().bestErrorMillis()).isEqualTo(3000L);
            });
        }

        @Test
        void 이번달_블라인드타이머_기록이_없으면_빈_리스트를_반환한다() {
            // when
            final List<BlindTimerTopPlayerResponse> result = dashboardService.getBlindTimerTopPlayers();

            // then
            assertThat(result).isEmpty();
        }
    }
}
