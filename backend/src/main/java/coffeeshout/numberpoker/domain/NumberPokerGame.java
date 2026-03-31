package coffeeshout.numberpoker.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.player.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NumberPokerGame {

    private static final int DEFAULT_ROUND_COUNT = 3;
    private static final int MIN_ROUND_COUNT = 1;
    private static final int MAX_ROUND_COUNT = 5;
    private static final int CARDS_PER_HAND = 2;

    private final List<Player> players;
    private int totalRounds;
    private int currentRoundNumber;
    private PokerPhase currentPhase;
    private PokerRound currentRound;

    public NumberPokerGame(List<Player> players) {
        this.players = List.copyOf(players);
        this.totalRounds = DEFAULT_ROUND_COUNT;
        this.currentRoundNumber = 0;
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

    public void startRound(Random random) {
        this.currentRoundNumber++;
        final List<PokerCard> deck = buildShuffledDeck(random);
        final Dealer dealer = new Dealer(deck.remove(0), deck.remove(0));
        final Map<Player, PlayerPokerHand> hands = new HashMap<>();
        for (Player player : players) {
            hands.put(player, new PlayerPokerHand(player, deck.remove(0), deck.remove(0)));
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

    public void markReady(Player player) {
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

    public PokerPhase getCurrentPhase() {
        return currentPhase;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public List<Player> getPlayers() {
        return players;
    }

    private void requirePhase(PokerPhase... allowedPhases) {
        for (PokerPhase allowed : allowedPhases) {
            if (currentPhase == allowed) {
                return;
            }
        }
        throw new BusinessException(
                NumberPokerErrorCode.INVALID_PHASE_ACTION,
                "현재 페이즈에서 허용되지 않는 액션입니다. currentPhase=" + currentPhase
        );
    }

    private List<PokerCard> buildShuffledDeck(Random random) {
        final List<PokerCard> deck = new ArrayList<>();
        for (int value = 1; value <= 10; value++) {
            for (int copy = 0; copy < 4; copy++) {
                deck.add(new PokerCard(value));
            }
        }
        Collections.shuffle(deck, random);
        return deck;
    }
}
