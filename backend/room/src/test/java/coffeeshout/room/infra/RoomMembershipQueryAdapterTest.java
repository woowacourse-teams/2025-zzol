package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.friend.domain.RoomMembership;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.MemoryRoomRepository;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 방 스캔으로 "누가 어느 방에 있고 그 방에 들어갈 수 있는가"를 만드는 책임만 검증한다.
 * 저장소가 인메모리라 목으로 바꿔 얻을 게 없어 실제 {@link MemoryRoomRepository}를 쓴다.
 */
class RoomMembershipQueryAdapterTest {

    private static final Long 호스트_ID = 1L;
    private static final Long 게스트_ID = 2L;
    private static final Long 방에_없는_유저_ID = 99L;

    private MemoryRoomRepository roomRepository;
    private RoomMembershipQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        roomRepository = new MemoryRoomRepository();
        adapter = new RoomMembershipQueryAdapter(roomRepository);
    }

    private Room 로비_방(String joinCode, Long hostUserId) {
        final Room room = new Room(new JoinCode(joinCode), new PlayerName("호스트"), hostUserId, 0.7);
        return roomRepository.save(room);
    }

    @Nested
    @DisplayName("참여 중인 방 조회")
    class 참여_중인_방_조회 {

        @Test
        @DisplayName("방에 참여 중인 사용자의 참여 코드를 반환한다")
        void 방에_참여_중인_사용자의_참여_코드를_반환한다() {
            로비_방("ABCD", 호스트_ID);

            final Map<Long, RoomMembership> result = adapter.findByUserIds(List.of(호스트_ID));

            assertThat(result.get(호스트_ID).joinCode()).isEqualTo("ABCD");
        }

        @Test
        @DisplayName("어느 방에도 없는 사용자는 결과에 담기지 않는다")
        void 어느_방에도_없는_사용자는_결과에_담기지_않는다() {
            로비_방("ABCD", 호스트_ID);

            final Map<Long, RoomMembership> result = adapter.findByUserIds(List.of(호스트_ID, 방에_없는_유저_ID));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).containsKey(호스트_ID);
                softly.assertThat(result).doesNotContainKey(방에_없는_유저_ID);
            });
        }

        @Test
        @DisplayName("여러 방에 흩어진 사용자를 각자의 방으로 매핑한다")
        void 여러_방에_흩어진_사용자를_각자의_방으로_매핑한다() {
            로비_방("ABCD", 호스트_ID);
            로비_방("BCDF", 게스트_ID);

            final Map<Long, RoomMembership> result = adapter.findByUserIds(List.of(호스트_ID, 게스트_ID));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.get(호스트_ID).joinCode()).isEqualTo("ABCD");
                softly.assertThat(result.get(게스트_ID).joinCode()).isEqualTo("BCDF");
            });
        }

        @Test
        @DisplayName("로그인하지 않은 플레이어는 조회되지 않는다")
        void 로그인하지_않은_플레이어는_조회되지_않는다() {
            final Room room = new Room(new JoinCode("ABCD"), new PlayerName("익명호스트"), 0.7);
            roomRepository.save(room);

            final Map<Long, RoomMembership> result = adapter.findByUserIds(List.of(호스트_ID));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("조회 대상이 없으면 빈 맵을 반환한다")
        void 조회_대상이_없으면_빈_맵을_반환한다() {
            로비_방("ABCD", 호스트_ID);

            final Map<Long, RoomMembership> result = adapter.findByUserIds(List.of());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("입장 가능 여부 판정")
    class 입장_가능_여부_판정 {

        @Test
        @DisplayName("로비 상태이고 정원에 여유가 있으면 입장 가능하다")
        void 로비_상태이고_정원에_여유가_있으면_입장_가능하다() {
            로비_방("ABCD", 호스트_ID);

            final Map<Long, RoomMembership> result = adapter.findByUserIds(List.of(호스트_ID));

            assertThat(result.get(호스트_ID).joinable()).isTrue();
        }

        @Test
        @DisplayName("게임이 시작된 방은 참여 코드는 남기고 입장 불가로 표시한다")
        void 게임이_시작된_방은_참여_코드는_남기고_입장_불가로_표시한다() {
            final Room room = 로비_방("ABCD", 호스트_ID);
            room.markPlaying();

            final Map<Long, RoomMembership> result = adapter.findByUserIds(List.of(호스트_ID));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.get(호스트_ID).joinCode()).isEqualTo("ABCD");
                softly.assertThat(result.get(호스트_ID).joinable()).isFalse();
            });
        }

        @Test
        @DisplayName("정원이 찬 방은 입장 불가로 표시한다")
        void 정원이_찬_방은_입장_불가로_표시한다() {
            final Room room = 로비_방("ABCD", 호스트_ID);
            // 호스트 1명 + 게스트 8명 = 정원 9명
            for (int i = 1; i <= 8; i++) {
                room.joinGuest(new PlayerName("게스트" + i));
            }

            final Map<Long, RoomMembership> result = adapter.findByUserIds(List.of(호스트_ID));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.get(호스트_ID).joinCode()).isEqualTo("ABCD");
                softly.assertThat(result.get(호스트_ID).joinable()).isFalse();
            });
        }
    }
}
