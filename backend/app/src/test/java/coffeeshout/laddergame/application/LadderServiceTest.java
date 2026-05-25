package coffeeshout.laddergame.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.support.app.ServiceTest;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class LadderServiceTest extends ServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private LadderService service;

    @MockitoSpyBean
    private LadderNotifier notifier;

    private static final String HOST_NAME = "꾹이";

    private Room room;
    private LadderGame game;
    private JoinCode joinCode;

    @BeforeEach
    void setUp() {
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));

        game = new LadderGame();
        room.addMiniGame(new PlayerName(HOST_NAME), game);
        room.startNextGame(HOST_NAME);
        game.changeToPrepare();
        game.changeToDrawing();

        roomRepository.save(room);
        joinCode = room.getJoinCode();
    }

    @Nested
    class drawLine_정상_요청 {

        @Test
        void 유효한_선_긋기_요청은_notifier를_호출한다() {
            service.drawLine(joinCode.getValue(), HOST_NAME, 0);

            verify(notifier, times(1)).notifyLineDrawn(any(), any());
        }

        @Test
        void 각_플레이어가_독립적으로_선을_그을_수_있다() {
            service.drawLine(joinCode.getValue(), HOST_NAME, 0);
            service.drawLine(joinCode.getValue(), "루키", 1);

            verify(notifier, times(2)).notifyLineDrawn(any(), any());
        }

        @Test
        void 선_긋기_후_lines_size가_증가한다() {
            service.drawLine(joinCode.getValue(), HOST_NAME, 0);

            assertThat(game.getLines().size()).isEqualTo(1);
        }
    }

    @Nested
    class drawLine_검증_실패_무시 {

        @Test
        void DRAWING_상태가_아니면_notifier를_호출하지_않는다() {
            game.changeToResult();

            service.drawLine(joinCode.getValue(), HOST_NAME, 0);

            verify(notifier, never()).notifyLineDrawn(any(), any());
        }

        @Test
        void 이미_선을_그은_플레이어_재요청은_notifier를_호출하지_않는다() {
            service.drawLine(joinCode.getValue(), HOST_NAME, 0);

            service.drawLine(joinCode.getValue(), HOST_NAME, 1);

            verify(notifier, times(1)).notifyLineDrawn(any(), any());
        }

        @Test
        void 미참여자_요청은_notifier를_호출하지_않는다() {
            service.drawLine(joinCode.getValue(), "없는플레이어", 0);

            verify(notifier, never()).notifyLineDrawn(any(), any());
        }

        @Test
        void 유효하지_않은_segmentIndex는_notifier를_호출하지_않는다() {
            // 기둥 4개(꾹이+루키+엠제이+한스) → 유효한 구간: 0,1,2 → 3은 유효하지 않음
            service.drawLine(joinCode.getValue(), HOST_NAME, 3);

            verify(notifier, never()).notifyLineDrawn(any(), any());
        }
    }

    @Nested
    class getMiniGameType {

        @Test
        void LADDER_GAME_타입을_반환한다() {
            assertThat(service.getMiniGameType().name()).isEqualTo("LADDER_GAME");
        }
    }
}
