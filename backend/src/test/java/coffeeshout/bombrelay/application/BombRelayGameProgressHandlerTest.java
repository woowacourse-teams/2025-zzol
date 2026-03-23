package coffeeshout.bombrelay.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.bombrelay.domain.BombRelayGame;
import coffeeshout.bombrelay.domain.BombRelayGameState;
import coffeeshout.bombrelay.domain.KoreanCharUtils;
import coffeeshout.bombrelay.domain.event.BombRelayProgressEvent;
import coffeeshout.bombrelay.domain.event.WordResultEvent;
import coffeeshout.bombrelay.infra.WordValidator;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class BombRelayGameProgressHandlerTest extends ServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BombRelayGameProgressHandler progressHandler;

    @MockitoBean
    private WordValidator wordValidator;

    private static final String HOST_NAME = "꾹이";

    private Room room;
    private BombRelayGame game;
    private String joinCode;

    @BeforeEach
    void setUp() {
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
        game = new BombRelayGame();
        room.addMiniGame(new PlayerName(HOST_NAME), game);
        room.startNextGame(HOST_NAME);
        joinCode = room.getJoinCode().getValue();

        game.setUp(room.getPlayers());
        game.startRound();
        game.startPlaying();
    }

    @Nested
    class 단어_수락 {

        @Test
        void 사전에_존재하는_유효한_단어를_입력하면_수락된다() {
            // given
            when(wordValidator.isValidWord(anyString())).thenReturn(true);
            final String currentTurnName = game.getCurrentTurnPlayer().getName();
            final String validWord = makeValidWord(game.getCurrentWord());

            // when
            progressHandler.handleWord(joinCode, currentTurnName, validWord);

            // then
            assertThat(game.getCurrentWord()).isEqualTo(validWord);

            final ArgumentCaptor<WordResultEvent> captor = ArgumentCaptor.forClass(WordResultEvent.class);
            verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
            assertThat(captor.getValue().accepted()).isTrue();
            verify(eventPublisher, atLeastOnce()).publishEvent(any(BombRelayProgressEvent.class));
        }

        @Test
        void 단어_수락_후_턴이_다음_플레이어로_넘어간다() {
            // given
            when(wordValidator.isValidWord(anyString())).thenReturn(true);
            final String firstTurnName = game.getCurrentTurnPlayer().getName();
            final String validWord = makeValidWord(game.getCurrentWord());

            // when
            progressHandler.handleWord(joinCode, firstTurnName, validWord);

            // then
            assertThat(game.getCurrentTurnPlayer().getName()).isNotEqualTo(firstTurnName);
        }
    }

    @Nested
    class 단어_거절_로컬_검증 {

        @Test
        void 현재_턴이_아닌_플레이어가_입력하면_사전_API_호출_없이_거절된다() {
            // given
            final String currentTurnName = game.getCurrentTurnPlayer().getName();
            final String otherName = findOtherPlayerName(currentTurnName);

            // when
            progressHandler.handleWord(joinCode, otherName, "아무단어");

            // then
            verify(wordValidator, never()).isValidWord(anyString());
            final ArgumentCaptor<WordResultEvent> captor = ArgumentCaptor.forClass(WordResultEvent.class);
            verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
            assertThat(captor.getValue().accepted()).isFalse();
        }

        @Test
        void 한_글자_단어는_사전_API_호출_없이_거절된다() {
            // given
            final String currentTurnName = game.getCurrentTurnPlayer().getName();

            // when
            progressHandler.handleWord(joinCode, currentTurnName, "가");

            // then
            verify(wordValidator, never()).isValidWord(anyString());
        }
    }

    @Nested
    class 단어_거절_사전_검증 {

        @Test
        void 사전에_존재하지_않는_단어는_거절된다() {
            // given
            when(wordValidator.isValidWord(anyString())).thenReturn(false);
            final String currentTurnName = game.getCurrentTurnPlayer().getName();
            final String validWord = makeValidWord(game.getCurrentWord());

            // when
            progressHandler.handleWord(joinCode, currentTurnName, validWord);

            // then
            verify(wordValidator).isValidWord(validWord);
            assertThat(game.getCurrentWord()).isNotEqualTo(validWord);

            final ArgumentCaptor<WordResultEvent> captor = ArgumentCaptor.forClass(WordResultEvent.class);
            verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
            assertThat(captor.getValue().accepted()).isFalse();
            assertThat(captor.getValue().rejectReason()).contains("사전");
        }
    }

    private String makeValidWord(String previousWord) {
        final char lastChar = KoreanCharUtils.getLastChar(previousWord);
        return lastChar + "자";
    }

    private String findOtherPlayerName(String currentTurnName) {
        return room.getPlayers().stream()
                .map(p -> p.getName().value())
                .filter(name -> !name.equals(currentTurnName))
                .findFirst()
                .orElseThrow();
    }
}
