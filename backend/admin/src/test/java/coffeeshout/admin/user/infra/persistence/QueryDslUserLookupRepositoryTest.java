package coffeeshout.admin.user.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.admin.user.domain.UserActivity;
import coffeeshout.admin.user.domain.UserListRow;
import coffeeshout.admin.user.domain.UserLookupRepository;
import coffeeshout.admin.user.domain.UserPlayAggregate;
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
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@DisplayName("QueryDslUserLookupRepository")
class QueryDslUserLookupRepositoryTest extends AdminModuleServiceTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

    @Autowired
    private UserLookupRepository userLookupRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private RouletteResultJpaRepository rouletteResultJpaRepository;

    @Autowired
    private MiniGameJpaRepository miniGameJpaRepository;

    @Autowired
    private MiniGameResultJpaRepository miniGameResultJpaRepository;

    private void play(RoomEntity room, PlayerEntity player, MiniGameType type) {
        final MiniGameEntity miniGamePlay = miniGameJpaRepository.save(new MiniGameEntity(room.getId(), type));
        miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGamePlay, player.getId(), 1, 100L));
    }

    @Nested
    class search {

        @Test
        void 닉네임_부분_일치로_찾는다() {
            userJpaRepository.save(new UserEntity("AB3CD", "김철수"));
            userJpaRepository.save(new UserEntity("XY4ZQ", "박영희"));

            assertThat(userLookupRepository.search("철수", FIRST_PAGE).getContent())
                    .extracting(UserListRow::nickname)
                    .containsExactly("김철수");
        }

        @Test
        void 목록에_플레이_수와_가장_많이_한_게임이_함께_온다() {
            // 목록에서 판단이 끝나는 일이 많아 상세를 열지 않고도 활동량이 보여야 한다.
            //
            // 같은 사람이 방 두 개에 걸쳐 세 판을 한 상황을 만든다. 판 수를 방과 조인해
            // 세면 방 수만큼 판이 불어난다. 여기서는 판이 정확히 셋이어야 한다.
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final UserEntity other = userJpaRepository.save(new UserEntity("XY4ZQ", "영희"));
            final RoomEntity room1 = roomJpaRepository.save(new RoomEntity("AAAA"));
            final RoomEntity room2 = roomJpaRepository.save(new RoomEntity("BBBB"));
            final PlayerEntity host =
                    playerJpaRepository.save(new PlayerEntity(room1, "철수", PlayerType.HOST, user.getId()));
            final PlayerEntity again =
                    playerJpaRepository.save(new PlayerEntity(room2, "철수", PlayerType.GUEST, user.getId()));
            final PlayerEntity otherPlayer =
                    playerJpaRepository.save(new PlayerEntity(room2, "영희", PlayerType.HOST, other.getId()));
            play(room1, host, MiniGameType.CARD_GAME);
            play(room2, again, MiniGameType.CARD_GAME);
            play(room2, again, MiniGameType.RACING_GAME);
            play(room2, otherPlayer, MiniGameType.RACING_GAME);

            assertThat(userLookupRepository.search("철수", FIRST_PAGE).getContent())
                    .extracting(UserListRow::id, UserListRow::playCount, UserListRow::topGame)
                    .containsExactly(tuple(user.getId(), 3L, MiniGameType.CARD_GAME.name()));
        }

        @Test
        void 같은_방에_여러_행이_있어도_목록에_한_줄만_나온다() {
            // 조인으로 활동량을 세면 그 사람의 행이 불어나 목록에 같은 유저가 두 줄로 나온다.
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("AAAA"));
            playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST, user.getId()));
            playerJpaRepository.save(new PlayerEntity(room, "철수2", PlayerType.GUEST, user.getId()));

            assertThat(userLookupRepository.search("철수", FIRST_PAGE).getContent())
                    .extracting(UserListRow::id)
                    .containsExactly(user.getId());
        }

        @Test
        void 한_판도_안_한_사람은_플레이_수가_0이고_게임이_없다() {
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("AAAA"));
            playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST, user.getId()));

            final UserListRow row =
                    userLookupRepository.search("철수", FIRST_PAGE).getContent().getFirst();

            assertThat(row.playCount()).isZero();
            assertThat(row.topGame()).isNull();
            assertThat(row.lastPlayedAt()).isNotNull();
        }

        @Test
        void 유저코드는_완전_일치로만_찾는다() {
            // 5자짜리 코드에 부분 일치를 걸면 아무 두 글자로도 수십 명이 걸려 검색이 쓸모없어진다.
            userJpaRepository.save(new UserEntity("AB3CD", "철수"));

            assertThat(userLookupRepository.search("AB3CD", FIRST_PAGE).getContent())
                    .hasSize(1);
            assertThat(userLookupRepository.search("AB3", FIRST_PAGE).getContent())
                    .isEmpty();
        }

        @Test
        void 유저코드는_소문자로_검색해도_찾는다() {
            userJpaRepository.save(new UserEntity("AB3CD", "철수"));

            assertThat(userLookupRepository.search("ab3cd", FIRST_PAGE).getContent())
                    .hasSize(1);
        }

        @Test
        void 키워드가_비어_있으면_최근_가입부터_전체를_돌려준다() {
            userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            userJpaRepository.save(new UserEntity("XY4ZQ", "영희"));

            assertThat(userLookupRepository.search("", FIRST_PAGE).getTotalElements())
                    .isEqualTo(2);
        }

        @Test
        void 탈퇴_회원은_조회되지_않는다() {
            // UserEntity 의 @SQLRestriction("deleted_at IS NULL") 이 모든 JPA 조회에 붙는다.
            // 백오피스가 활성 회원만 본다는 사실을 이 테스트가 고정한다.
            final UserEntity active = userJpaRepository.save(new UserEntity("AB3CD", "활성"));
            final UserEntity withdrawn = userJpaRepository.save(new UserEntity("XY4ZQ", "탈퇴"));
            withdrawn.softDelete();
            userJpaRepository.saveAndFlush(withdrawn);

            assertThat(userLookupRepository.search(null, FIRST_PAGE).getContent())
                    .extracting(UserListRow::id)
                    .containsExactly(active.getId());
        }
    }

    @Nested
    class findActivity {

        @Test
        void 참여한_방_수와_당첨_수를_센다() {
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final RoomEntity room1 = roomJpaRepository.save(new RoomEntity("AAAA"));
            final RoomEntity room2 = roomJpaRepository.save(new RoomEntity("BBBB"));
            final PlayerEntity p1 =
                    playerJpaRepository.save(new PlayerEntity(room1, "철수", PlayerType.HOST, user.getId()));
            playerJpaRepository.save(new PlayerEntity(room2, "철수", PlayerType.GUEST, user.getId()));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room1, p1, 25));

            final UserActivity activity = userLookupRepository.findActivity(user.getId());

            assertThat(activity.roomCount()).isEqualTo(2);
            assertThat(activity.winCount()).isEqualTo(1);
            assertThat(activity.winRate()).isEqualTo(0.5);
        }

        @Test
        void 같은_방에_여러_행이_있어도_방은_한_번만_센다() {
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("AAAA"));
            playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST, user.getId()));
            playerJpaRepository.save(new PlayerEntity(room, "철수2", PlayerType.GUEST, user.getId()));

            assertThat(userLookupRepository.findActivity(user.getId()).roomCount())
                    .isEqualTo(1);
        }

        @Test
        void 참여_이력이_없으면_전부_0이다() {
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));

            final UserActivity activity = userLookupRepository.findActivity(user.getId());

            assertThat(activity.roomCount()).isZero();
            assertThat(activity.winCount()).isZero();
            assertThat(activity.winRate()).isZero();
        }

        @Test
        void 다른_유저의_당첨은_세지_않는다() {
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final UserEntity other = userJpaRepository.save(new UserEntity("XY4ZQ", "영희"));
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("AAAA"));
            playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST, user.getId()));
            final PlayerEntity otherPlayer =
                    playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST, other.getId()));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, otherPlayer, 50));

            assertThat(userLookupRepository.findActivity(user.getId()).winCount())
                    .isZero();
        }
    }

    @Nested
    class aggregatePlays {

        @Test
        void 사람마다_판_수와_마지막_참여를_한_줄로_준다() {
            // 판 수는 mini_game_result 에, 마지막 참여는 player 에 있다. 둘을 한 쿼리로
            // 조인하면 판 수만큼 player 행이 불어나 방 참여가 여러 번 세진다.
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final RoomEntity room1 = roomJpaRepository.save(new RoomEntity("AAAA"));
            final RoomEntity room2 = roomJpaRepository.save(new RoomEntity("BBBB"));
            final PlayerEntity first =
                    playerJpaRepository.save(new PlayerEntity(room1, "철수", PlayerType.HOST, user.getId()));
            final PlayerEntity second =
                    playerJpaRepository.save(new PlayerEntity(room2, "철수", PlayerType.GUEST, user.getId()));
            play(room1, first, MiniGameType.CARD_GAME);
            play(room2, second, MiniGameType.RACING_GAME);

            assertThat(userLookupRepository.aggregatePlays())
                    .extracting(UserPlayAggregate::userId, UserPlayAggregate::playCount)
                    .containsExactly(tuple(user.getId(), 2L));
            assertThat(userLookupRepository.aggregatePlays().getFirst().lastPlayedAt())
                    .isNotNull();
        }

        @Test
        void 탈퇴_회원은_빠진다() {
            // player.user_id 만 보고 세면 탈퇴 회원이 섞인다. @SQLRestriction 은 그 엔티티가
            // 쿼리에 등장할 때 붙는 것이라 id 만 들고 다니면 걸리지 않는다. 같은 화면의
            // 회원 수는 탈퇴를 빼고 세므로, 섞이면 분포의 합이 회원 수를 넘어선다.
            final UserEntity active = userJpaRepository.save(new UserEntity("AB3CD", "활성"));
            final UserEntity withdrawn = userJpaRepository.save(new UserEntity("XY4ZQ", "탈퇴"));
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("AAAA"));
            final PlayerEntity stayed =
                    playerJpaRepository.save(new PlayerEntity(room, "활성", PlayerType.HOST, active.getId()));
            final PlayerEntity left =
                    playerJpaRepository.save(new PlayerEntity(room, "탈퇴", PlayerType.GUEST, withdrawn.getId()));
            play(room, stayed, MiniGameType.CARD_GAME);
            play(room, left, MiniGameType.CARD_GAME);
            withdrawn.softDelete();
            userJpaRepository.saveAndFlush(withdrawn);

            assertThat(userLookupRepository.aggregatePlays())
                    .extracting(UserPlayAggregate::userId)
                    .containsExactly(active.getId());
        }

        @Test
        void 게스트는_회원과_이을_수_없어_빠진다() {
            // player.user_id 가 없는 참여자다. 세면 회원 수보다 많은 분포가 나온다.
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("AAAA"));
            playerJpaRepository.save(new PlayerEntity(room, "손님", PlayerType.GUEST, null));

            assertThat(userLookupRepository.aggregatePlays()).isEmpty();
        }

        @Test
        void 방에만_들어오고_한_판도_안_한_사람은_판_수가_0이다() {
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("AAAA"));
            playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST, user.getId()));

            assertThat(userLookupRepository.aggregatePlays())
                    .extracting(UserPlayAggregate::userId, UserPlayAggregate::playCount)
                    .containsExactly(tuple(user.getId(), 0L));
        }
    }

    @Nested
    class findSignupTimes {

        @Test
        void 기간_밖_가입은_빠진다() {
            userJpaRepository.save(new UserEntity("AB3CD", "철수"));

            final Instant now = Instant.now();

            assertThat(userLookupRepository.findSignupTimes(now.minus(Duration.ofDays(1)), now.plusSeconds(60)))
                    .hasSize(1);
            assertThat(userLookupRepository.findSignupTimes(
                            now.minus(Duration.ofDays(10)), now.minus(Duration.ofDays(9))))
                    .isEmpty();
        }
    }

    @Nested
    class findProviders {

        @Test
        void 연결된_제공자가_없으면_빈_목록이다() {
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수"));

            assertThat(userLookupRepository.findProviders(user.getId())).isEmpty();
        }
    }
}
