package coffeeshout.room.application.event;

import static org.mockito.Mockito.verify;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.application.service.RoomCommandService;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MiniGameResultRoomListenerTest {

    private static final String JOIN_CODE = "AB3C";

    @InjectMocks
    MiniGameResultRoomListener listener;

    @Mock
    RoomCommandService roomCommandService;

    @Captor
    ArgumentCaptor<Map<PlayerName, Integer>> rankCaptor;

    @Test
    @DisplayName("이름 기반 순위 맵을 PlayerName 맵으로 변환해 applyGameResult를 호출한다")
    void 이름_순위_맵을_PlayerName_맵으로_변환해_위임한다() {
        // given
        MiniGameFinishedEvent event = new MiniGameFinishedEvent(
                JOIN_CODE,
                MiniGameType.CARD_GAME.name(),
                Map.of("한스", 1, "루키", 2),
                3
        );

        // when
        listener.handle(event);

        // then
        verify(roomCommandService).applyGameResult(
                org.mockito.ArgumentMatchers.eq(new JoinCode(JOIN_CODE)),
                rankCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(3)
        );
        org.assertj.core.api.Assertions.assertThat(rankCaptor.getValue())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        new PlayerName("한스"), 1,
                        new PlayerName("루키"), 2
                ));
    }
}
