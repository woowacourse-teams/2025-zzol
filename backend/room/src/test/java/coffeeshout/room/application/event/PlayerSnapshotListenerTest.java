package coffeeshout.room.application.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.PlayerSnapshotRequiredEvent;
import coffeeshout.room.application.port.PlayerEntityRepository;
import coffeeshout.room.application.port.RoomEntityRepository;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.RoomEntity;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerSnapshotListenerTest {

    private static final String JOIN_CODE = "A4BX";

    @Mock
    private RoomQueryService roomQueryService;

    @Mock
    private RoomEntityRepository roomEntityRepository;

    @Mock
    private PlayerEntityRepository playerEntityRepository;

    @Mock
    private RoomEntity roomEntity;

    private PlayerSnapshotListener listener;

    @BeforeEach
    void setUp() {
        listener = new PlayerSnapshotListener(roomQueryService, roomEntityRepository, playerEntityRepository);
    }

    @Nested
    @DisplayName("PlayerSnapshotRequiredEvent를 수신하면")
    class 이벤트_수신 {

        @Test
        @DisplayName("방의 모든 플레이어에 대해 같은 RoomEntity를 앵커로 PlayerEntity를 생성·저장한다")
        void 모든_플레이어를_저장한다() {
            // given
            final Room room = RoomFixture.호스트_꾹이(new JoinCode(JOIN_CODE));
            given(roomEntityRepository.findFirstByJoinCodeOrderByCreatedAtDesc(JOIN_CODE))
                    .willReturn(Optional.of(roomEntity));
            given(roomQueryService.getByJoinCode(new JoinCode(JOIN_CODE))).willReturn(room);

            // when
            listener.handle(new PlayerSnapshotRequiredEvent(JOIN_CODE));

            // then
            final ArgumentCaptor<PlayerEntity> captor = ArgumentCaptor.forClass(PlayerEntity.class);
            verify(playerEntityRepository, times(room.getPlayers().size())).save(captor.capture());

            final List<String> savedNames = captor.getAllValues().stream()
                    .map(PlayerEntity::getPlayerName)
                    .toList();
            final List<String> expectedNames = room.getPlayers().stream()
                    .map(player -> player.getName().value())
                    .toList();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedNames).containsExactlyInAnyOrderElementsOf(expectedNames);
                softly.assertThat(captor.getAllValues())
                        .extracting(PlayerEntity::getRoomSession)
                        .containsOnly(roomEntity);
            });
        }

        @Test
        @DisplayName("방이 존재하지 않으면 예외를 던진다")
        void 방이_없으면_예외() {
            // given
            given(roomEntityRepository.findFirstByJoinCodeOrderByCreatedAtDesc(JOIN_CODE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> listener.handle(new PlayerSnapshotRequiredEvent(JOIN_CODE)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
