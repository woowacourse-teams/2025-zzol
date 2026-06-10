package coffeeshout.room.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import coffeeshout.gamecommon.GameRoomHostChangedEvent;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.application.service.RoomCommandService;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code removePlayer}의 호스트 승계 → GameSession 동기화 이벤트 발행 책임만 검증하는 단위 테스트.
 * 발행/미발행 상호작용 검증이 핵심이라 {@code DelayedRoomRemovalServiceTest}와 동일하게 순수 Mockito로 구성한다.
 * 승계 동작(promoteNewHost)을 실제로 돌려야 before/after 호스트 비교가 의미를 가지므로 Room은 실제 도메인 객체를 쓴다.
 */
@ExtendWith(MockitoExtension.class)
class RoomCommandServiceHostPromotionTest {

    private static final JoinCode JOIN_CODE = new JoinCode("ABCD");
    private static final PlayerName HOST = new PlayerName("호스트");
    private static final PlayerName GUEST_1 = new PlayerName("게스트1");
    private static final PlayerName GUEST_2 = new PlayerName("게스트2");

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomQueryService roomQueryService;

    @Mock
    private StreamPublisher streamPublisher;

    @InjectMocks
    private RoomCommandService roomCommandService;

    // 첫 입장 순서: 호스트 → 게스트1 → 게스트2. 호스트가 떠나면 남은 첫 플레이어(게스트1)가 승계된다.
    private Room threePlayerRoom() {
        final Room room = new Room(JOIN_CODE, HOST, 0.7);
        room.joinGuest(GUEST_1);
        room.joinGuest(GUEST_2);
        return room;
    }

    @Nested
    @DisplayName("호스트 변경 이벤트 발행(removePlayer)")
    class PublishHostChange {

        @Test
        @DisplayName("호스트가 떠나면 승계된 새 호스트 이름으로 GameRoomHostChangedEvent를 발행한다")
        void 호스트가_떠나면_승계된_새_호스트_이름으로_이벤트를_발행한다() {
            // given
            given(roomQueryService.getByJoinCode(JOIN_CODE)).willReturn(threePlayerRoom());

            // when
            final boolean removed = roomCommandService.removePlayer(JOIN_CODE, HOST);

            // then
            assertThat(removed).isTrue();
            final ArgumentCaptor<GameRoomHostChangedEvent> captor =
                    ArgumentCaptor.forClass(GameRoomHostChangedEvent.class);
            then(streamPublisher).should().publish(eq(RoomStreamKey.BROADCAST), captor.capture());
            assertThat(captor.getValue().joinCode()).isEqualTo(JOIN_CODE.getValue());
            assertThat(captor.getValue().newHostName()).isEqualTo(GUEST_1.value());
        }

        @Test
        @DisplayName("비호스트가 떠나면 호스트가 그대로이므로 이벤트를 발행하지 않는다")
        void 비호스트가_떠나면_이벤트를_발행하지_않는다() {
            // given
            given(roomQueryService.getByJoinCode(JOIN_CODE)).willReturn(threePlayerRoom());

            // when
            final boolean removed = roomCommandService.removePlayer(JOIN_CODE, GUEST_2);

            // then
            assertThat(removed).isTrue();
            verifyNoInteractions(streamPublisher);
        }

        @Test
        @DisplayName("마지막 플레이어(호스트)가 떠나 방이 비면 삭제만 하고 호스트 변경 이벤트는 발행하지 않는다")
        void 마지막_플레이어가_떠나_방이_비면_이벤트를_발행하지_않는다() {
            // given — 호스트 혼자 남은 방
            final Room soloRoom = new Room(JOIN_CODE, HOST, 0.7);
            given(roomQueryService.getByJoinCode(JOIN_CODE)).willReturn(soloRoom);

            // when
            final boolean removed = roomCommandService.removePlayer(JOIN_CODE, HOST);

            // then
            assertThat(removed).isTrue();
            then(roomRepository).should().deleteByJoinCode(JOIN_CODE);
            verifyNoInteractions(streamPublisher);
        }

        @Test
        @DisplayName("존재하지 않는 플레이어 제거는 호스트 불변이므로 이벤트를 발행하지 않는다")
        void 존재하지_않는_플레이어_제거는_이벤트를_발행하지_않는다() {
            // given
            given(roomQueryService.getByJoinCode(JOIN_CODE)).willReturn(threePlayerRoom());

            // when
            final boolean removed = roomCommandService.removePlayer(JOIN_CODE, new PlayerName("없는사람"));

            // then
            assertThat(removed).isFalse();
            verifyNoInteractions(streamPublisher);
        }
    }
}
