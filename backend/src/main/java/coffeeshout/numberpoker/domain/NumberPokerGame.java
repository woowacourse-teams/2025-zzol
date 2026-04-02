package coffeeshout.numberpoker.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.player.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class NumberPokerGame implements Playable {

    private static final int DEFAULT_ROUND_COUNT = 3;
    private static final int MIN_ROUND_COUNT = 1;
    private static final int MAX_ROUND_COUNT = 5;
    private static final int MIN_CARD_VALUE = 1;
    private static final int MAX_CARD_VALUE = 10;
    private static final int CARDS_PER_VALUE = 3;

    private List<Player> players;
    private int totalRounds;
    private int currentRoundNumber;
    private int skippedRounds;
    private PokerPhase currentPhase;
    private PokerRound currentRound;

    public NumberPokerGame() {
        this.players = List.of();
        this.totalRounds = DEFAULT_ROUND_COUNT;
        this.currentRoundNumber = 0;
        this.skippedRounds = 0;
        this.currentPhase = null;
    }

    public NumberPokerGame(List<Player> players) {
        this.players = List.copyOf(players);
        this.totalRounds = DEFAULT_ROUND_COUNT;
        this.currentRoundNumber = 0;
        this.skippedRounds = 0;
        this.currentPhase = null;
    }

    public void configureRoundCount(int roundCount) {
        if (roundCount < MIN_ROUND_COUNT || roundCount > MAX_ROUND_COUNT) {
            throw new BusinessException(
                    NumberPokerErrorCode.INVALID_ROUND_COUNT,
                    "라운드 수는 1~5이어야 합니다. roundCount=" + roundCount
            );
        }
        this.totalRounds = roundCount;
    }

    public void startRound(DeckShuffler shuffler) {
        this.currentRoundNumber++;
        final List<PokerCard> deck = buildShuffledDeck(shuffler);
        final Dealer dealer = new Dealer(deck.removeFirst(), deck.removeFirst());
        final Map<Player, PlayerPokerHand> hands = new HashMap<>();
        for (Player player : players) {
            hands.put(player, new PlayerPokerHand(player, deck.removeFirst(), deck.removeFirst()));
        }
        this.currentRound = new PokerRound(currentRoundNumber, totalRounds, dealer, hands);
        this.currentPhase = PokerPhase.LOADING;
    }

    public void beginStage1() {
        this.currentPhase = PokerPhase.STAGE_1;
    }

    public void fold(Player player) {
        requirePhase(PokerPhase.STAGE_1, PokerPhase.STAGE_2);
        currentRound.fold(player, currentPhase);
    }

    public void beginStage2() {
        currentRound.getDealer().revealFirst();
        this.currentPhase = PokerPhase.STAGE_2;
    }

    public void showdown() {
        currentRound.getDealer().revealAll();
        this.currentPhase = PokerPhase.SHOWDOWN;
    }

    public void scoreBoard() {
        this.currentPhase = PokerPhase.SCORE_BOARD;
    }

    public void beginRoundReady() {
        this.currentPhase = PokerPhase.ROUND_READY;
    }

    public void done() {
        this.currentPhase = PokerPhase.DONE;
    }

    public void markReady(Player player) {
        if (currentRound == null) {
            throw new BusinessException(
                    NumberPokerErrorCode.ROUND_NOT_IN_PROGRESS,
                    "진행 중인 라운드가 없습니다."
            );
        }
        requirePhase(PokerPhase.ROUND_READY);
        currentRound.markReady(player);
    }

    public boolean isAllReady() {
        return currentRound.isAllReady(players);
    }

    public Map<Player, PokerRoundResult> getCurrentRoundResults() {
        return currentRound.calculateResults();
    }

    public boolean isAllFolded() {
        return currentRound.isAllFolded();
    }

    public boolean isPlayerFolded(Player player) {
        return currentRound.isPlayerFolded(player);
    }

    public boolean hasCurrentRound() {
        return currentRound != null;
    }

    public boolean isFirstRound() {
        return currentRound != null && currentRound.isFirst();
    }

    public boolean isLastRound() {
        return currentRound != null && currentRound.isLast();
    }

    public int getCurrentRoundNumber() {
        return currentRound != null ? currentRound.getNumber() : 0;
    }

    public int getEffectiveRoundNumber() {
        return getCurrentRoundNumber() + skippedRounds;
    }

    public void addSkippedRound() {
        this.skippedRounds++;
    }

    public void resetSkippedRounds() {
        this.skippedRounds = 0;
    }

    public List<PokerCard> getDealerVisibleCards() {
        if (currentRound == null) {
            return List.of();
        }
        return currentRound.getDealer().getVisibleCards();
    }

    public int getDealerHiddenCount() {
        if (currentRound == null) {
            return 0;
        }
        return currentRound.getDealer().getHiddenCount();
    }

    public boolean isPlayerReady(Player player) {
        if (currentRound == null) {
            return false;
        }
        return currentRound.isPlayerReady(player);
    }

    public int getFoldCount() {
        if (currentRound == null) {
            return 0;
        }
        return currentRound.getFoldCount();
    }

    public Map<Player, HandRanking> getActivePlayerHandRankings() {
        if (currentRound == null) {
            return Map.of();
        }
        return currentRound.getActivePlayerHandRankings();
    }

    public int[] getPlayerCardValues(Player player) {
        return currentRound.getPlayerCardValues(player);
    }

    private void requirePhase(PokerPhase... allowed) {
        for (PokerPhase phase : allowed) {
            if (currentPhase == phase) {
                return;
            }
        }
        throw new BusinessException(
                NumberPokerErrorCode.INVALID_PHASE_ACTION,
                "현재 페이즈에서 허용되지 않는 액션입니다. currentPhase=" + currentPhase
        );
    }

    private List<PokerCard> buildShuffledDeck(DeckShuffler shuffler) {
        final List<PokerCard> deck = new ArrayList<>();
        for (int value = MIN_CARD_VALUE; value <= MAX_CARD_VALUE; value++) {
            for (int copy = 0; copy < CARDS_PER_VALUE; copy++) {
                deck.add(new PokerCard(value));
            }
        }
        shuffler.shuffle(deck);
        return deck;
    }

    @Override
    public void setUp(List<Player> players) {
        this.players = List.copyOf(players);
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.NUMBER_POKER;
    }

    @Override
    public MiniGameResult getResult() {
        // Number Poker는 자체 확률 조정 로직(NumberPokerProbabilityAdjuster)을 사용하므로
        // MiniGameResult는 placeholder로 모든 플레이어에게 동일 순위 부여
        final Map<Player, Integer> allFirst = players.stream()
                .collect(Collectors.toMap(Function.identity(), p -> 1));
        return new MiniGameResult(allFirst);
    }

    @Override
    public Map<Player, MiniGameScore> getScores() {
        return players.stream()
                .collect(Collectors.toMap(Function.identity(), p -> new NumberPokerScore()));
    }

    @Override
    public boolean shouldAdjustProbabilities() {
        return false;
    }
}
