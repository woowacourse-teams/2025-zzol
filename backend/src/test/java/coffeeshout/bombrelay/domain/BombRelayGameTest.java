package coffeeshout.bombrelay.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BombRelayGameTest {

    private BombRelayGame game;
    private Player 한스;
    private Player 꾹이;
    private Player 루키;

    @BeforeEach
    void setUp() {
        game = new BombRelayGame();
        한스 = PlayerFixture.게스트한스();
        꾹이 = PlayerFixture.게스트꾹이();
        루키 = PlayerFixture.게스트루키();
        game.setUp(List.of(한스, 꾹이, 루키));
    }

    @Nested
    class 게임_초기_상태 {

        @Test
        void 초기_상태는_DESCRIPTION이다() {
            assertThat(game.getState()).isEqualTo(BombRelayGameState.DESCRIPTION);
        }

        @Test
        void getMiniGameType은_BOMB_RELAY를_반환한다() {
            assertThat(game.getMiniGameType()).isEqualTo(MiniGameType.BOMB_RELAY);
        }

        @Test
        void 세명이면_최대_2라운드이다() {
            assertThat(game.getMaxRounds()).isEqualTo(2);
        }
    }

    @Nested
    class 라운드_시작 {

        @Test
        void startRound하면_라운드_번호가_증가한다() {
            game.startRound();
            assertThat(game.getCurrentRound()).isEqualTo(1);

            game.startRound();
            assertThat(game.getCurrentRound()).isEqualTo(2);
        }

        @Test
        void startRound하면_시작_단어가_설정된다() {
            game.startRound();
            assertThat(game.getCurrentWord()).isNotNull().isNotEmpty();
        }

        @Test
        void startRound하면_사용된_단어에_시작_단어가_포함된다() {
            game.startRound();
            final String startWord = game.getCurrentWord();
            assertThat(game.getUsedWords()).contains(startWord);
        }

        @Test
        void startRound하면_턴_인덱스가_0으로_초기화된다() {
            game.startRound();
            game.startPlaying();

            // 턴 진행
            final String currentTurnName = game.getCurrentTurnPlayer().getName();
            final String word = makeValidWord(game.getCurrentWord());
            game.validateWord(currentTurnName, word);
            game.acceptWord(word);

            // 새 라운드 시작
            game.startRound();
            assertThat(game.getTurnIndex()).isEqualTo(0);
        }
    }

    @Nested
    class 단어_검증 {

        @BeforeEach
        void startGame() {
            game.startRound();
            game.startPlaying();
        }

        @Test
        void PLAYING이_아니면_예외가_발생한다() {
            final BombRelayGame newGame = new BombRelayGame();
            newGame.setUp(List.of(한스));

            assertThatThrownBy(() -> newGame.validateWord("한스", "테스트"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void 현재_턴이_아닌_플레이어가_입력하면_거절된다() {
            final String currentTurnName = game.getCurrentTurnPlayer().getName();
            final String otherName = findOtherPlayerName(currentTurnName);

            final WordValidationResult result = game.validateWord(otherName, "아무단어");
            assertThat(result.isRejected()).isTrue();
            assertThat(result.errorCode()).isEqualTo(BombRelayGameErrorCode.NOT_CURRENT_TURN);
        }

        @Test
        void 한_글자_단어는_거절된다() {
            final String currentTurnName = game.getCurrentTurnPlayer().getName();

            final WordValidationResult result = game.validateWord(currentTurnName, "가");
            assertThat(result.isRejected()).isTrue();
            assertThat(result.errorCode()).isEqualTo(BombRelayGameErrorCode.SINGLE_CHAR_WORD);
        }

        @Test
        void 이전_단어_마지막_글자로_시작하지_않으면_거절된다() {
            final String currentTurnName = game.getCurrentTurnPlayer().getName();

            // 현재 단어의 마지막 글자와 무관한 단어
            final WordValidationResult result = game.validateWord(currentTurnName, "ㅋㅋㅋ");
            assertThat(result.isRejected()).isTrue();
        }

        @Test
        void 이미_사용된_단어는_거절된다() {
            final String currentTurnName = game.getCurrentTurnPlayer().getName();
            final String currentWord = game.getCurrentWord();
            final char lastChar = KoreanCharUtils.getLastChar(currentWord);

            // lastChar + "자" → "자자" → "자자" 재시도로 ALREADY_USED_WORD 검증
            final String word1 = lastChar + "자";
            game.acceptWord(word1);

            // "자자"를 acceptWord → currentWord가 "자자" (끝 글자 '자')
            game.acceptWord("자자");

            // "자자"를 다시 시도 → 끝 글자 '자'로 시작 ✓, usedWords에 이미 있음 → ALREADY_USED_WORD
            final String thirdTurnName = game.getCurrentTurnPlayer().getName();
            final WordValidationResult result = game.validateWord(thirdTurnName, "자자");

            assertThat(result.isRejected()).isTrue();
            assertThat(result.errorCode()).isEqualTo(BombRelayGameErrorCode.ALREADY_USED_WORD);
        }

        @Test
        void 유효한_단어는_사전_검증이_필요하다() {
            final String currentTurnName = game.getCurrentTurnPlayer().getName();
            final String validWord = makeValidWord(game.getCurrentWord());

            final WordValidationResult result = game.validateWord(currentTurnName, validWord);
            assertThat(result.requiresDictionaryCheck()).isTrue();
        }
    }

    @Nested
    class 단어_수락_및_턴_전환 {

        @BeforeEach
        void startGame() {
            game.startRound();
            game.startPlaying();
        }

        @Test
        void acceptWord하면_currentWord가_변경된다() {
            final String word = makeValidWord(game.getCurrentWord());
            game.acceptWord(word);
            assertThat(game.getCurrentWord()).isEqualTo(word);
        }

        @Test
        void acceptWord하면_turnIndex가_증가한다() {
            assertThat(game.getTurnIndex()).isEqualTo(0);
            game.acceptWord(makeValidWord(game.getCurrentWord()));
            assertThat(game.getTurnIndex()).isEqualTo(1);
        }

        @Test
        void acceptWord하면_사용된_단어에_추가된다() {
            final String word = makeValidWord(game.getCurrentWord());
            game.acceptWord(word);
            assertThat(game.getUsedWords()).contains(word);
        }
    }

    @Nested
    class 탈락_및_게임_종료 {

        @BeforeEach
        void startGame() {
            game.startRound();
            game.startPlaying();
        }

        @Test
        void eliminateCurrentPlayer로_현재_턴_플레이어가_탈락한다() {
            final String eliminatedName = game.getCurrentTurnPlayer().getName();
            game.eliminateCurrentPlayer();

            assertThat(game.getCurrentTurnPlayer().getName()).isNotEqualTo(eliminatedName);
            assertThat(game.getSurvivorCount()).isEqualTo(2);
        }

        @Test
        void maxRounds만큼_라운드가_진행되면_게임_종료이다() {
            // 3명 → 2라운드
            assertThat(game.isGameOver()).isFalse();

            // 1라운드 완료
            game.eliminateCurrentPlayer();
            assertThat(game.isGameOver()).isFalse();

            // 2라운드 시작 + 완료
            game.startRound();
            game.eliminateCurrentPlayer();
            assertThat(game.isGameOver()).isTrue();
        }

        @Test
        void tryFinish는_PLAYING에서_DONE으로_전이한다() {
            assertThat(game.tryFinish()).isTrue();
            assertThat(game.getState()).isEqualTo(BombRelayGameState.DONE);
        }

        @Test
        void tryFinish는_두번째_호출부터_false이다() {
            game.tryFinish();
            assertThat(game.tryFinish()).isFalse();
        }
    }

    @Nested
    class 랭킹_산정 {

        @Test
        void 생존자는_공동_1등이다() {
            game.startRound();
            game.startPlaying();
            game.eliminateCurrentPlayer();

            // 1라운드 탈락자 1명, 생존자 2명
            game.startRound();
            game.eliminateCurrentPlayer();

            // 2라운드 탈락자 1명, 생존자 1명
            final MiniGameResult result = game.getResult();
            final long rank1Count = result.getRank().values().stream().filter(r -> r == 1).count();
            assertThat(rank1Count).isEqualTo(1); // 생존자 1명 = 1등
        }

        @Test
        void 먼저_탈락한_사람이_낮은_순위를_받는다() {
            game.startRound();
            game.startPlaying();

            final Player firstEliminated = game.getCurrentTurnPlayer().getPlayer();
            game.eliminateCurrentPlayer();

            game.startRound();
            final Player secondEliminated = game.getCurrentTurnPlayer().getPlayer();
            game.eliminateCurrentPlayer();

            final MiniGameResult result = game.getResult();
            assertThat(result.getPlayerRank(firstEliminated))
                    .isGreaterThan(result.getPlayerRank(secondEliminated));
        }
    }

    /**
     * 주어진 단어의 마지막 글자로 시작하는 2글자 테스트 단어를 만든다.
     * 실제 사전에 있는 단어가 아닐 수 있지만, 도메인 로컬 검증 테스트에서는 충분하다.
     */
    private String makeValidWord(String previousWord) {
        final char lastChar = KoreanCharUtils.getLastChar(previousWord);
        return lastChar + "자";
    }

    private String findOtherPlayerName(String currentTurnName) {
        return List.of("한스", "꾹이", "루키").stream()
                .filter(name -> !name.equals(currentTurnName))
                .findFirst()
                .orElseThrow();
    }
}
