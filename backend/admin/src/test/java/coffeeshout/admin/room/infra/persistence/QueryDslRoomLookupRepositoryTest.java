package coffeeshout.admin.room.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.admin.room.domain.RoomLookupRepository;
import coffeeshout.admin.room.domain.RoomPlayer;
import coffeeshout.admin.room.domain.RoomSnapshot;
import coffeeshout.admin.room.domain.RoomSummary;
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
import coffeeshout.room.infra.persistence.RouletteResultEntity;
import coffeeshout.room.infra.persistence.RouletteResultJpaRepository;
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * QueryDSL 쿼리는 실제 DB 에서만 검증된다. 특히 참여자 수 서브쿼리와 게스트 leftJoin 은
 * 모킹으로는 아무것도 재지 못한다.
 */
@DisplayName("QueryDslRoomLookupRepository")
class QueryDslRoomLookupRepositoryTest extends AdminModuleServiceTest {

    @Autowired
    private RoomLookupRepository roomLookupRepository;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private RouletteResultJpaRepository rouletteResultJpaRepository;

    @Autowired
    private MiniGameJpaRepository miniGameJpaRepository;

    @Autowired
    private MiniGameResultJpaRepository miniGameResultJpaRepository;

    @Nested
    class search {

        @Test
        void 참여자_수를_서브쿼리로_함께_돌려준다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));
            playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST));

            final List<RoomSummary> found =
                    roomLookupRepository.search("ABCD", PageRequest.of(0, 20)).getContent();

            assertThat(found)
                    .singleElement()
                    .extracting(RoomSummary::playerCount)
                    .isEqualTo(2L);
        }

        @Test
        void 참여자가_없는_방도_0으로_나온다() {
            roomJpaRepository.save(new RoomEntity("EMPT"));

            assertThat(roomLookupRepository
                            .search("EMPT", PageRequest.of(0, 20))
                            .getContent())
                    .singleElement()
                    .extracting(RoomSummary::playerCount)
                    .isEqualTo(0L);
        }

        @Test
        void 같은_코드의_방이_여러_개면_최근_방부터_돌려준다() {
            // join_code 는 유니크가 아니다. 재사용된 코드로 검색하면 여러 방이 나온다.
            final RoomEntity older = roomJpaRepository.save(new RoomEntity("SAME"));
            final RoomEntity newer = roomJpaRepository.save(new RoomEntity("SAME"));

            assertThat(roomLookupRepository
                            .search("SAME", PageRequest.of(0, 20))
                            .getContent())
                    .extracting(RoomSummary::id)
                    .containsExactly(newer.getId(), older.getId());
        }

        @Test
        void 코드가_비어_있으면_전체를_돌려준다() {
            roomJpaRepository.save(new RoomEntity("AAAA"));
            roomJpaRepository.save(new RoomEntity("BBBB"));

            assertThat(roomLookupRepository.search("", PageRequest.of(0, 20)).getTotalElements())
                    .isEqualTo(2);
        }

        @Test
        void 소문자로_검색해도_찾는다() {
            roomJpaRepository.save(new RoomEntity("ABCD"));

            assertThat(roomLookupRepository
                            .search("abcd", PageRequest.of(0, 20))
                            .getContent())
                    .hasSize(1);
        }
    }

    @Nested
    class findPlayers {

        @Test
        void 게스트도_함께_돌려준다() {
            // leftJoin 이 아니면 user_id 가 없는 게스트가 통째로 사라진다.
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "철수닉"));
            playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST, user.getId()));
            playerJpaRepository.save(new PlayerEntity(room, "손님", PlayerType.GUEST));

            final List<RoomPlayer> players = roomLookupRepository.findPlayers(room.getId());

            assertThat(players).hasSize(2);
        }

        @Test
        void 로그인_사용자는_계정_닉네임을_함께_싣는다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            final UserEntity user = userJpaRepository.save(new UserEntity("AB3CD", "계정닉네임"));
            playerJpaRepository.save(new PlayerEntity(room, "방에서쓴이름", PlayerType.HOST, user.getId()));

            final RoomPlayer player =
                    roomLookupRepository.findPlayers(room.getId()).getFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(player.playerName()).isEqualTo("방에서쓴이름");
                softly.assertThat(player.nickname()).isEqualTo("계정닉네임");
                softly.assertThat(player.userCode()).isEqualTo("AB3CD");
            });
        }

        @Test
        void 게스트는_계정_정보가_비어_있다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            playerJpaRepository.save(new PlayerEntity(room, "손님", PlayerType.GUEST));

            final RoomPlayer player =
                    roomLookupRepository.findPlayers(room.getId()).getFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(player.userId()).isNull();
                softly.assertThat(player.nickname()).isNull();
                softly.assertThat(player.userCode()).isNull();
            });
        }
    }

    @Nested
    class findMiniGameResults {

        @Test
        void 게임_결과에_플레이어_이름을_붙여_돌려준다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            final PlayerEntity player = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));
            final MiniGameEntity miniGame =
                    miniGameJpaRepository.save(new MiniGameEntity(room.getId(), MiniGameType.RACING_GAME));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, player.getId(), 1, 500L));

            assertThat(roomLookupRepository.findMiniGameResults(room.getId()))
                    .singleElement()
                    .satisfies(result -> {
                        assertThat(result.playerName()).isEqualTo("철수");
                        assertThat(result.rank()).isEqualTo(1);
                    });
        }

        @Test
        void 다른_방의_결과는_섞이지_않는다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            final RoomEntity other = roomJpaRepository.save(new RoomEntity("WXYZ"));
            final PlayerEntity otherPlayer = playerJpaRepository.save(new PlayerEntity(other, "남", PlayerType.HOST));
            final MiniGameEntity miniGame =
                    miniGameJpaRepository.save(new MiniGameEntity(other.getId(), MiniGameType.RACING_GAME));
            miniGameResultJpaRepository.save(new MiniGameResultEntity(miniGame, otherPlayer.getId(), 1, 500L));

            assertThat(roomLookupRepository.findMiniGameResults(room.getId())).isEmpty();
        }
    }

    @Nested
    class findSnapshots {

        @Test
        void 참여자가_여러_명이어도_방은_한_줄이다() {
            // player 를 조인하면 참여자 수만큼 방이 불어나 방 수 자체가 틀어진다.
            // 이 화면은 방을 세는 자리다.
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST, null));
            playerJpaRepository.save(new PlayerEntity(room, "영희", PlayerType.GUEST, null));
            playerJpaRepository.save(new PlayerEntity(room, "민수", PlayerType.GUEST, null));

            final LocalDateTime now = LocalDateTime.now();

            assertThat(roomLookupRepository.findSnapshots(now.minusDays(1), now.plusDays(1)))
                    .extracting(RoomSnapshot::playerCount)
                    .containsExactly(3L);
        }

        @Test
        void 기간_밖의_방은_빠진다() {
            roomJpaRepository.save(new RoomEntity("ABCD"));

            final LocalDateTime now = LocalDateTime.now();

            assertThat(roomLookupRepository.findSnapshots(now.minusDays(10), now.minusDays(9)))
                    .isEmpty();
        }

        @Test
        void 끝난_방은_종료_시각이_함께_온다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            room.finish();
            roomJpaRepository.saveAndFlush(room);

            final LocalDateTime now = LocalDateTime.now();

            assertThat(roomLookupRepository.findSnapshots(now.minusDays(1), now.plusDays(1)))
                    .extracting(RoomSnapshot::status, snapshot -> snapshot.finishedAt() != null)
                    .containsExactly(tuple(RoomState.DONE, true));
        }

        @Test
        void 진행_중인_방은_종료_시각이_없다() {
            roomJpaRepository.save(new RoomEntity("ABCD"));

            final LocalDateTime now = LocalDateTime.now();

            assertThat(roomLookupRepository.findSnapshots(now.minusDays(1), now.plusDays(1)))
                    .extracting(RoomSnapshot::finishedAt)
                    .containsOnlyNulls();
        }
    }

    @Nested
    class findRouletteResult {

        @Test
        void 당첨자와_확률을_돌려준다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));
            final PlayerEntity winner = playerJpaRepository.save(new PlayerEntity(room, "철수", PlayerType.HOST));
            rouletteResultJpaRepository.save(new RouletteResultEntity(room, winner, 12));

            assertThat(roomLookupRepository.findRouletteResult(room.getId()))
                    .get()
                    .satisfies(result -> {
                        assertThat(result.winnerPlayerName()).isEqualTo("철수");
                        assertThat(result.winnerProbability()).isEqualTo(12);
                    });
        }

        @Test
        void 룰렛까지_못_간_방은_비어_있다() {
            final RoomEntity room = roomJpaRepository.save(new RoomEntity("ABCD"));

            assertThat(roomLookupRepository.findRouletteResult(room.getId())).isEmpty();
        }
    }
}
