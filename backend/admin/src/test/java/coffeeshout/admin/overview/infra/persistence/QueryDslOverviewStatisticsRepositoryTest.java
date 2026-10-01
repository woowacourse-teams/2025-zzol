package coffeeshout.admin.overview.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.admin.overview.domain.DailyTrendPoint;
import coffeeshout.admin.overview.domain.OverviewStatisticsRepository;
import coffeeshout.admin.overview.domain.OverviewStatisticsRepository.GamePlayCount;
import coffeeshout.admin.overview.domain.RoomFunnel;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultEntity;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DATE() 그룹핑과 distinct 집계는 실행해 봐야 안다.
 * 특히 게임별 판 수는 참가자 수만큼 결과 행이 생겨 distinct 를 빼면 부풀어 오른다.
 */
@DisplayName("QueryDslOverviewStatisticsRepository")
class QueryDslOverviewStatisticsRepositoryTest extends AdminModuleServiceTest {

    private static final LocalDateTime WIDE_FROM = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime WIDE_TO = LocalDateTime.of(2999, 1, 1, 0, 0);

    @Autowired
    private OverviewStatisticsRepository overviewStatisticsRepository;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private MiniGameJpaRepository miniGameJpaRepository;

    @Autowired
    private MiniGameResultJpaRepository miniGameResultJpaRepository;

    private RoomEntity room(String joinCode, RoomState status) {
        final RoomEntity room = roomJpaRepository.save(new RoomEntity(joinCode));
        if (status == RoomState.DONE) {
            room.finish();
        } else if (status != RoomState.READY) {
            room.updateRoomStatus(status);
        }
        return roomJpaRepository.save(room);
    }

    @Nested
    class findDailyTrend {

        @Test
        void 같은_날의_방을_하나로_묶는다() {
            room("AAAA", RoomState.READY);
            room("BBBB", RoomState.READY);

            final List<DailyTrendPoint> trend = overviewStatisticsRepository.findDailyTrend(WIDE_FROM, WIDE_TO);

            assertThat(trend)
                    .singleElement()
                    .extracting(DailyTrendPoint::created)
                    .isEqualTo(2L);
        }

        @Test
        void 상태가_섞여도_생성과_완주를_따로_센다() {
            // 날짜와 상태로 함께 묶은 뒤 자바에서 접는 부분이 실제로 맞는지 본다.
            room("AAAA", RoomState.DONE);
            room("BBBB", RoomState.DONE);
            room("CCCC", RoomState.PLAYING);
            room("DDDD", RoomState.READY);

            final DailyTrendPoint point = overviewStatisticsRepository
                    .findDailyTrend(WIDE_FROM, WIDE_TO)
                    .getFirst();

            assertThat(point.created()).isEqualTo(4L);
            assertThat(point.completed()).isEqualTo(2L);
        }

        @Test
        void 참여자_수는_방_수에_영향을_주지_않는다() {
            // 한 쿼리로 조인하면 참여자 수만큼 방이 부풀어 생성 수가 틀린다.
            final RoomEntity room = room("AAAA", RoomState.READY);
            playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));
            playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST));
            playerJpaRepository.save(new PlayerEntity(room, "민수", PlayerType.GUEST));

            final DailyTrendPoint point = overviewStatisticsRepository
                    .findDailyTrend(WIDE_FROM, WIDE_TO)
                    .getFirst();

            assertThat(point.created()).isEqualTo(1L);
            assertThat(point.players()).isEqualTo(3L);
        }

        @Test
        void 기간에_아무것도_없으면_빈_목록이다() {
            room("AAAA", RoomState.READY);

            assertThat(overviewStatisticsRepository.findDailyTrend(WIDE_FROM, LocalDateTime.of(2001, 1, 1, 0, 0)))
                    .isEmpty();
        }
    }

    @Nested
    class findFunnelBetween {

        @Test
        void 도달한_최대_단계까지_아래_단계를_모두_센다() {
            // roomStatus 는 방이 도달한 최대 단계 하나만 들고 있다. DONE 인 방을
            // "완주"에만 세고 "게임 시작"에서 빠뜨리면 퍼널이 아래로 갈수록 늘어난다.
            room("AAAA", RoomState.DONE);

            final RoomFunnel funnel = overviewStatisticsRepository.findFunnelBetween(WIDE_FROM, WIDE_TO);

            assertThat(funnel.created()).isEqualTo(1L);
            assertThat(funnel.gameStarted()).isEqualTo(1L);
            assertThat(funnel.rouletteReached()).isEqualTo(1L);
            assertThat(funnel.completed()).isEqualTo(1L);
        }

        @Test
        void 단계가_뒤로_갈수록_줄어든다() {
            room("AAAA", RoomState.READY);
            room("BBBB", RoomState.PLAYING);
            room("CCCC", RoomState.ROULETTE);
            room("DDDD", RoomState.DONE);

            final RoomFunnel funnel = overviewStatisticsRepository.findFunnelBetween(WIDE_FROM, WIDE_TO);

            assertThat(funnel.created()).isEqualTo(4L);
            assertThat(funnel.gameStarted()).isEqualTo(3L);
            assertThat(funnel.rouletteReached()).isEqualTo(2L);
            assertThat(funnel.completed()).isEqualTo(1L);
        }

        @Test
        void 미니게임_기록이_없는_방은_완료로_세지_않는다() {
            // 게임 시작과 이 단계의 차이가 곧 "하다가 나간 방"이다.
            // mini_game_play 행은 게임이 끝날 때 생기므로, 시작만 한 방은 여기서 빠진다.
            room("AAAA", RoomState.PLAYING);

            final RoomEntity finished = room("BBBB", RoomState.DONE);
            final MiniGameEntity play =
                    miniGameJpaRepository.save(new MiniGameEntity(finished.getId(), MiniGameType.CARD_GAME));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(play, 1L, 1, 100L));

            final RoomFunnel funnel = overviewStatisticsRepository.findFunnelBetween(WIDE_FROM, WIDE_TO);

            assertThat(funnel.gameStarted()).isEqualTo(2L);
            assertThat(funnel.miniGamePlayed()).isEqualTo(1L);
        }

        @Test
        void 한_방에서_여러_판을_해도_한_번만_센다() {
            // 조인으로 세면 판 수만큼 방이 부풀어 퍼널이 앞 단계보다 커진다.
            final RoomEntity room = room("AAAA", RoomState.DONE);
            for (MiniGameType type :
                    List.of(MiniGameType.CARD_GAME, MiniGameType.RACING_GAME, MiniGameType.WORM_GAME)) {
                final MiniGameEntity play = miniGameJpaRepository.save(new MiniGameEntity(room.getId(), type));
                miniGameResultJpaRepository.save(new MiniGameResultEntity(play, 1L, 1, 100L));
            }

            assertThat(overviewStatisticsRepository
                            .findFunnelBetween(WIDE_FROM, WIDE_TO)
                            .miniGamePlayed())
                    .isEqualTo(1L);
        }

        @Test
        void 시작만_하고_끝내지_못한_판은_완료로_세지_않는다() {
            // mini_game_play 행은 게임이 시작될 때 쌓인다. 그 행만 보고 세면 시작한 방까지
            // "미니게임 완료"가 되어, 이 단계가 직전 단계인 "게임 시작"보다 커진다.
            final RoomEntity room = room("AAAA", RoomState.PLAYING);
            miniGameJpaRepository.save(new MiniGameEntity(room.getId(), MiniGameType.CARD_GAME));

            final RoomFunnel funnel = overviewStatisticsRepository.findFunnelBetween(WIDE_FROM, WIDE_TO);

            assertThat(funnel.gameStarted()).isEqualTo(1L);
            assertThat(funnel.miniGamePlayed()).isZero();
        }

        @Test
        void 완료_단계는_시작_단계를_넘지_않는다() {
            // 퍼널이 106%를 찍은 적이 있다. 단계가 앞 단계보다 크면 읽는 사람은 어느 숫자를
            // 믿어야 할지 알 수 없고, 그 순간 나머지 단계까지 못 믿게 된다.
            //
            // READY 방에 판 행을 붙여 그 상태를 그대로 재현한다. 판 행으로 세면 이 방까지
            // "완료"에 들어가 시작(2)보다 완료(3)가 커진다.
            final RoomEntity ready = room("AAAA", RoomState.READY);
            miniGameJpaRepository.save(new MiniGameEntity(ready.getId(), MiniGameType.NUNCHI_GAME));

            final RoomEntity playing = room("BBBB", RoomState.PLAYING);
            miniGameJpaRepository.save(new MiniGameEntity(playing.getId(), MiniGameType.CARD_GAME));

            final RoomEntity done = room("CCCC", RoomState.DONE);
            final MiniGameEntity play =
                    miniGameJpaRepository.save(new MiniGameEntity(done.getId(), MiniGameType.RACING_GAME));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(play, 1L, 1, 100L));

            final RoomFunnel funnel = overviewStatisticsRepository.findFunnelBetween(WIDE_FROM, WIDE_TO);

            assertThat(funnel.gameStarted()).isEqualTo(2L);
            assertThat(funnel.miniGamePlayed()).isEqualTo(1L).isLessThanOrEqualTo(funnel.gameStarted());
        }

        @Test
        void 방이_하나도_없으면_나머지를_조회하지_않고_0을_돌려준다() {
            assertThat(overviewStatisticsRepository.findFunnelBetween(WIDE_FROM, WIDE_TO))
                    .isEqualTo(RoomFunnel.empty());
        }

        @Test
        void 상한은_배타다() {
            // 자정 정각에 생긴 방이 이틀에 걸쳐 두 번 세지면 안 된다.
            final RoomEntity room = room("AAAA", RoomState.READY);

            final LocalDateTime createdAt = room.getCreatedAt();
            assertThat(overviewStatisticsRepository
                            .findFunnelBetween(createdAt, createdAt)
                            .created())
                    .isZero();
            assertThat(overviewStatisticsRepository
                            .findFunnelBetween(createdAt, createdAt.plusSeconds(1))
                            .created())
                    .isEqualTo(1L);
        }
    }

    @Nested
    class countPlaysByGame {

        @Test
        void 참가자가_여럿이어도_한_판으로_센다() {
            // 한 판에 참가자 수만큼 결과 행이 생긴다. distinct 를 빼면 4명짜리 한 판이 4판이 된다.
            final RoomEntity room = room("AAAA", RoomState.DONE);
            final MiniGameEntity play =
                    miniGameJpaRepository.save(new MiniGameEntity(room.getId(), MiniGameType.RACING_GAME));
            for (int rank = 1; rank <= 4; rank++) {
                final PlayerEntity player =
                        playerJpaRepository.save(new PlayerEntity(room, "p" + rank, PlayerType.GUEST));
                miniGameResultJpaRepository.save(new MiniGameResultEntity(play, player.getId(), rank, 100L));
            }

            assertThat(overviewStatisticsRepository.countPlaysByGame(WIDE_FROM, WIDE_TO))
                    .singleElement()
                    .extracting(GamePlayCount::started, GamePlayCount::finished)
                    .containsExactly(1L, 1L);
        }

        @Test
        void 게임_타입별로_나눠_센다() {
            final RoomEntity room = room("AAAA", RoomState.DONE);
            final PlayerEntity player = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));

            final MiniGameEntity racing =
                    miniGameJpaRepository.save(new MiniGameEntity(room.getId(), MiniGameType.RACING_GAME));
            final MiniGameEntity blockStacking =
                    miniGameJpaRepository.save(new MiniGameEntity(room.getId(), MiniGameType.BLOCK_STACKING));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(racing, player.getId(), 1, 100L));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(blockStacking, player.getId(), 1, 200L));

            assertThat(overviewStatisticsRepository.countPlaysByGame(WIDE_FROM, WIDE_TO))
                    .hasSize(2)
                    .allSatisfy(count -> assertThat(count.started()).isEqualTo(1L));
        }

        @Test
        void 결과가_없는_판도_시작한_판으로_센다() {
            // 이 테스트가 이탈률의 근거다. mini_game_play 행은 게임이 시작될 때 쌓이므로
            // 결과가 한 줄도 없는 판이 남는다. 그 판이 조회에서 빠지면 이탈이 있었다는
            // 사실 자체가 화면에 안 보인다.
            final RoomEntity room = room("AAAA", RoomState.PLAYING);
            miniGameJpaRepository.save(new MiniGameEntity(room.getId(), MiniGameType.RACING_GAME));

            assertThat(overviewStatisticsRepository.countPlaysByGame(WIDE_FROM, WIDE_TO))
                    .singleElement()
                    .extracting(GamePlayCount::started, GamePlayCount::finished)
                    .containsExactly(1L, 0L);
        }

        @Test
        void 같은_게임의_깨진_판과_끝난_판을_한_줄에_모은다() {
            final RoomEntity room = room("AAAA", RoomState.PLAYING);
            final PlayerEntity player = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));

            final MiniGameEntity finished =
                    miniGameJpaRepository.save(new MiniGameEntity(room.getId(), MiniGameType.RACING_GAME));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(finished, player.getId(), 1, 100L));
            miniGameJpaRepository.save(new MiniGameEntity(room.getId(), MiniGameType.RACING_GAME));

            assertThat(overviewStatisticsRepository.countPlaysByGame(WIDE_FROM, WIDE_TO))
                    .singleElement()
                    .extracting(GamePlayCount::started, GamePlayCount::finished)
                    .containsExactly(2L, 1L);
        }

        @Test
        void 기간은_방_생성_시각으로_자른다() {
            // mini_game_play 에는 시각 컬럼이 없다. 결과 시각으로 자르면 결과가 없는 판은
            // 어느 기간에도 안 걸려 영영 안 보인다.
            final RoomEntity room = room("AAAA", RoomState.PLAYING);
            miniGameJpaRepository.save(new MiniGameEntity(room.getId(), MiniGameType.RACING_GAME));
            final LocalDateTime createdAt = room.getCreatedAt();

            assertThat(overviewStatisticsRepository.countPlaysByGame(createdAt, createdAt))
                    .isEmpty();
            assertThat(overviewStatisticsRepository.countPlaysByGame(createdAt, createdAt.plusSeconds(1)))
                    .singleElement()
                    .extracting(GamePlayCount::started)
                    .isEqualTo(1L);
        }

        @Test
        void 게임_기록이_없으면_빈_목록이다() {
            assertThat(overviewStatisticsRepository.countPlaysByGame(WIDE_FROM, WIDE_TO))
                    .isEmpty();
        }
    }
}
