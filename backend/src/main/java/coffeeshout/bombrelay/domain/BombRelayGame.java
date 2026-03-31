package coffeeshout.bombrelay.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.player.Player;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class BombRelayGame implements Playable {

    private static final String[] START_WORDS = {
            "사과", "바나나", "자동차", "컴퓨터", "학교",
            "기차", "도서관", "피아노", "카메라", "고양이",
            "토마토", "주사위", "거북이", "미소", "나비",
            "소나무", "다리미", "부채", "호랑이", "무지개",
            "가위", "나라", "다람쥐", "두부", "모자",
            "바다", "사자", "아기", "오리", "우유",
            "이불", "지구", "코끼리", "포도", "하마",
            "구두", "노래", "라면", "마차", "보리",
            "수박", "여우", "자라", "치마", "타조",
            "파리", "허리", "거미", "너구리", "어머니"
    };

    private BombRelayPlayers players;
    @Getter(AccessLevel.NONE)
    private final AtomicReference<BombRelayGameState> state =
            new AtomicReference<>(BombRelayGameState.DESCRIPTION);

    private int currentRound;
    private int maxRounds;
    private int turnIndex;
    private String currentWord;
    private final Set<String> usedWords = new HashSet<>();

    @Setter
    private ScheduledFuture<?> bombTimerFuture;

    public BombRelayGame() {
    }

    @Override
    public void setUp(List<Player> playerList) {
        this.players = new BombRelayPlayers(playerList);
        this.maxRounds = players.calculateMaxRounds();
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromAscending(getScores());
    }

    @Override
    public Map<Player, MiniGameScore> getScores() {
        return players.stream()
                .collect(Collectors.toMap(
                        BombRelayPlayer::getPlayer,
                        this::calculateScore
                ));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.BOMB_RELAY;
    }

    public void startRound() {
        this.currentRound++;
        this.turnIndex = 0;
        this.usedWords.clear();
        this.currentWord = pickStartWord();
        this.usedWords.add(currentWord);
    }

    public void startPlaying() {
        state.set(BombRelayGameState.PLAYING);
    }

    public void updateState(BombRelayGameState newState) {
        state.set(newState);
    }

    public BombRelayGameState getState() {
        return state.get();
    }

    public boolean tryFinish() {
        return state.compareAndSet(BombRelayGameState.PLAYING, BombRelayGameState.DONE);
    }

    public BombRelayPlayer getCurrentTurnPlayer() {
        return players.getByTurnIndex(turnIndex);
    }

    /**
     * 단어 유효성 검증 (사전 검증 제외).
     * 사전 검증은 서비스 레이어에서 외부 API로 처리.
     */
    public WordValidationResult validateWord(String playerName, String word) {
        validatePlaying();

        final BombRelayPlayer currentPlayer = getCurrentTurnPlayer();
        if (!currentPlayer.getName().equals(playerName)) {
            return WordValidationResult.rejected(BombRelayGameErrorCode.NOT_CURRENT_TURN);
        }

        if (word.length() < 2) {
            return WordValidationResult.rejected(BombRelayGameErrorCode.SINGLE_CHAR_WORD);
        }

        final char lastChar = KoreanCharUtils.getLastChar(currentWord);
        final char firstChar = KoreanCharUtils.getFirstChar(word);
        if (!KoreanCharUtils.isValidFirstChar(lastChar, firstChar)) {
            return WordValidationResult.rejected(BombRelayGameErrorCode.INVALID_FIRST_CHAR);
        }

        if (usedWords.contains(word)) {
            return WordValidationResult.rejected(BombRelayGameErrorCode.ALREADY_USED_WORD);
        }

        return WordValidationResult.needsDictionaryCheck();
    }

    public void acceptWord(String word) {
        usedWords.add(word);
        currentWord = word;
        turnIndex++;
    }

    public void eliminateCurrentPlayer() {
        getCurrentTurnPlayer().eliminate(currentRound);
    }

    public boolean isGameOver() {
        return currentRound >= maxRounds;
    }

    public void cancelBombTimer() {
        if (bombTimerFuture != null && !bombTimerFuture.isDone()) {
            bombTimerFuture.cancel(false);
        }
    }

    public List<BombRelayPlayer> getSurvivors() {
        return players.getSurvivors();
    }

    public int getSurvivorCount() {
        return players.survivorCount();
    }

    private MiniGameScore calculateScore(BombRelayPlayer player) {
        if (!player.isEliminated()) {
            return BombRelayScore.ofSurvivor();
        }
        return BombRelayScore.ofEliminated(player.getEliminatedRound(), maxRounds);
    }

    private void validatePlaying() {
        if (state.get() != BombRelayGameState.PLAYING) {
            throw new BusinessException(
                    BombRelayGameErrorCode.NOT_PLAYING_STATE,
                    "현재 게임 상태가 플레이 중이 아닙니다: " + state.get()
            );
        }
    }

    private String pickStartWord() {
        return START_WORDS[ThreadLocalRandom.current().nextInt(START_WORDS.length)];
    }
}
