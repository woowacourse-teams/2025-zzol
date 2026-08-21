package coffeeshout.room.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

import coffeeshout.friend.application.port.RoomMembershipQuery;
import coffeeshout.friend.domain.RoomMembership;
import coffeeshout.friend.domain.event.RoomPresenceChangedEvent;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 변이 직전·직후 두 스냅샷의 비교가 실제로 성립하는지 검증한다.
 * 입장·퇴장·게임 시작·정원참·방 삭제가 각각 <b>누구에게 무엇을</b> 내보내는지, 그리고 무관한 저장이
 * 아무것도 내보내지 않는지가 핵심이다.
 */
@ExtendWith(MockitoExtension.class)
class RoomPresencePublisherTest {

    private static final JoinCode JOIN_CODE = new JoinCode("ABCD");
    private static final Long 호스트_ID = 1L;
    private static final Long 게스트_ID = 2L;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RoomMembershipQuery roomMembershipQuery;

    @InjectMocks
    private RoomPresencePublisher roomPresencePublisher;

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room(JOIN_CODE, new PlayerName("호스트"), 호스트_ID, 0.7);
        // 기본값: 방을 떠난 사용자는 다른 방에도 없다
        lenient().when(roomMembershipQuery.findByUserIds(anyCollection())).thenReturn(Map.of());
    }

    private List<RoomPresenceChangedEvent> 발행된_이벤트() {
        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should(atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(RoomPresenceChangedEvent.class::isInstance)
                .map(RoomPresenceChangedEvent.class::cast)
                .toList();
    }

    @Nested
    @DisplayName("입장")
    class 입장 {

        @Test
        @DisplayName("방이 처음 만들어지면 호스트에게 참여 코드를 알린다")
        void 방이_처음_만들어지면_호스트에게_참여_코드를_알린다() {
            roomPresencePublisher.publish(RoomPresence.empty(JOIN_CODE), RoomPresence.of(room));

            final List<RoomPresenceChangedEvent> events = 발행된_이벤트();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(events).hasSize(1);
                softly.assertThat(events.getFirst().userId()).isEqualTo(호스트_ID);
                softly.assertThat(events.getFirst().membership())
                        .isEqualTo(new RoomMembership(JOIN_CODE.getValue(), true));
            });
        }

        @Test
        @DisplayName("게스트가 들어오면 새로 들어온 사람에게만 알린다")
        void 게스트가_들어오면_새로_들어온_사람에게만_알린다() {
            final RoomPresence before = RoomPresence.of(room);
            room.joinGuest(new PlayerName("게스트"), 게스트_ID);

            roomPresencePublisher.publish(before, RoomPresence.of(room));

            final List<RoomPresenceChangedEvent> events = 발행된_이벤트();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(events).hasSize(1);
                softly.assertThat(events.getFirst().userId()).isEqualTo(게스트_ID);
                softly.assertThat(events.getFirst().membership())
                        .isEqualTo(new RoomMembership(JOIN_CODE.getValue(), true));
            });
        }

        @Test
        @DisplayName("로그인하지 않은 게스트는 알림 대상이 아니다")
        void 로그인하지_않은_게스트는_알림_대상이_아니다() {
            final RoomPresence before = RoomPresence.of(room);
            room.joinGuest(new PlayerName("익명게스트"));

            roomPresencePublisher.publish(before, RoomPresence.of(room));

            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("퇴장·방 삭제")
    class 퇴장_방_삭제 {

        @Test
        @DisplayName("나간 사람에게는 참여 중인 방이 없다고 알린다")
        void 나간_사람에게는_참여_중인_방이_없다고_알린다() {
            room.joinGuest(new PlayerName("게스트"), 게스트_ID);
            final RoomPresence before = RoomPresence.of(room);
            room.removePlayer(new PlayerName("게스트"));

            roomPresencePublisher.publish(before, RoomPresence.of(room));

            final List<RoomPresenceChangedEvent> events = 발행된_이벤트();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(events).hasSize(1);
                softly.assertThat(events.getFirst().userId()).isEqualTo(게스트_ID);
                softly.assertThat(events.getFirst().membership()).isEqualTo(RoomMembership.NONE);
            });
        }

        @Test
        @DisplayName("방이 삭제되면 남아 있던 전원에게 참여 중인 방이 없다고 알린다")
        void 방이_삭제되면_남아_있던_전원에게_참여_중인_방이_없다고_알린다() {
            room.joinGuest(new PlayerName("게스트"), 게스트_ID);
            final RoomPresence before = RoomPresence.of(room);

            roomPresencePublisher.publish(before, RoomPresence.empty(JOIN_CODE));

            final List<RoomPresenceChangedEvent> events = 발행된_이벤트();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(events).hasSize(2);
                softly.assertThat(events)
                        .allSatisfy(event -> assertThat(event.membership()).isEqualTo(RoomMembership.NONE));
                softly.assertThat(events)
                        .extracting(RoomPresenceChangedEvent::userId)
                        .containsExactlyInAnyOrder(호스트_ID, 게스트_ID);
            });
        }

        @Test
        @DisplayName("퇴장자의 현재 소속은 인원수와 무관하게 한 번에 조회한다")
        void 퇴장자의_현재_소속은_인원수와_무관하게_한_번에_조회한다() {
            room.joinGuest(new PlayerName("게스트"), 게스트_ID);
            final RoomPresence before = RoomPresence.of(room);

            roomPresencePublisher.publish(before, RoomPresence.empty(JOIN_CODE));

            then(roomMembershipQuery).should().findByUserIds(Set.of(호스트_ID, 게스트_ID));
        }

        @Test
        @DisplayName("방을 옮긴 사용자에게는 새 방을 알린다")
        void 방을_옮긴_사용자에게는_새_방을_알린다() {
            room.joinGuest(new PlayerName("게스트"), 게스트_ID);
            final RoomPresence before = RoomPresence.of(room);
            room.removePlayer(new PlayerName("게스트"));
            // 퇴장 처리가 지연되는 동안 이미 다른 방에 들어가 있다
            given(roomMembershipQuery.findByUserIds(Set.of(게스트_ID)))
                    .willReturn(Map.of(게스트_ID, new RoomMembership("BCDF", true)));

            roomPresencePublisher.publish(before, RoomPresence.of(room));

            final List<RoomPresenceChangedEvent> events = 발행된_이벤트();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(events.getLast().userId()).isEqualTo(게스트_ID);
                softly.assertThat(events.getLast().membership()).isEqualTo(new RoomMembership("BCDF", true));
            });
        }

        @Test
        @DisplayName("이미 사라진 방의 삭제는 아무것도 발행하지 않는다")
        void 이미_사라진_방의_삭제는_아무것도_발행하지_않는다() {
            roomPresencePublisher.publish(RoomPresence.empty(JOIN_CODE), RoomPresence.empty(JOIN_CODE));

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("퇴장자 소속 조회가 실패해도 방 처리 흐름을 끊지 않는다")
        void 퇴장자_소속_조회가_실패해도_방_처리_흐름을_끊지_않는다() {
            room.joinGuest(new PlayerName("게스트"), 게스트_ID);
            final RoomPresence before = RoomPresence.of(room);
            given(roomMembershipQuery.findByUserIds(anyCollection())).willThrow(new RuntimeException("조회 실패"));

            assertThatCode(() -> roomPresencePublisher.publish(before, RoomPresence.empty(JOIN_CODE)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("입장 가능 여부 전이")
    class 입장_가능_여부_전이 {

        @Test
        @DisplayName("게임이 시작되면 남아 있는 전원에게 입장 불가를 알린다")
        void 게임이_시작되면_남아_있는_전원에게_입장_불가를_알린다() {
            room.joinGuest(new PlayerName("게스트"), 게스트_ID);
            final RoomPresence before = RoomPresence.of(room);
            room.markPlaying();

            roomPresencePublisher.publish(before, RoomPresence.of(room));

            final List<RoomPresenceChangedEvent> events = 발행된_이벤트();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(events).hasSize(2);
                softly.assertThat(events).allSatisfy(event -> assertThat(event.membership())
                        .isEqualTo(new RoomMembership(JOIN_CODE.getValue(), false)));
                softly.assertThat(events)
                        .extracting(RoomPresenceChangedEvent::userId)
                        .containsExactlyInAnyOrder(호스트_ID, 게스트_ID);
            });
        }

        @Test
        @DisplayName("정원이 차면 남아 있는 전원에게 입장 불가를 알린다")
        void 정원이_차면_남아_있는_전원에게_입장_불가를_알린다() {
            room.joinGuest(new PlayerName("게스트"), 게스트_ID);
            final RoomPresence before = RoomPresence.of(room);
            // 호스트 + 게스트 2명 → 정원 9명까지 7명 더 채운다
            for (int i = 1; i <= 7; i++) {
                room.joinGuest(new PlayerName("익명" + i));
            }

            roomPresencePublisher.publish(before, RoomPresence.of(room));

            final List<RoomPresenceChangedEvent> events = 발행된_이벤트();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(events).allSatisfy(event -> assertThat(event.membership())
                        .isEqualTo(new RoomMembership(JOIN_CODE.getValue(), false)));
                softly.assertThat(events)
                        .extracting(RoomPresenceChangedEvent::userId)
                        .containsExactlyInAnyOrder(호스트_ID, 게스트_ID);
            });
        }

        @Test
        @DisplayName("회원 구성과 입장 가능 여부가 그대로면 아무것도 발행하지 않는다")
        void 회원_구성과_입장_가능_여부가_그대로면_아무것도_발행하지_않는다() {
            roomPresencePublisher.publish(RoomPresence.of(room), RoomPresence.of(room));

            verifyNoInteractions(eventPublisher);
        }
    }
}
