package coffeeshout.numberpoker.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.player.Player;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PokerRound {

    private final int number;
    private final int total;
    private final Dealer dealer;
    private final Map<Player, PlayerPokerHand> playerHands;
    private final Set<Player> readyPlayers;

    public PokerRound(int number, int total, Dealer dealer, Map<Player, PlayerPokerHand> playerHands) {
        this.number = number;
        this.total = total;
        this.dealer = dealer;
        this.playerHands = new HashMap<>(playerHands);
        this.readyPlayers = new HashSet<>();
    }

    public void fold(Player player, PokerPhase currentPhase) {
        final PlayerPokerHand hand = findHand(player);
        hand.fold(currentPhase);
    }

    public boolean isPlayerFolded(Player player) {
        return findHand(player).isFolded();
    }

    public boolean isAllFolded() {
        return playerHands.values().stream().allMatch(PlayerPokerHand::isFolded);
    }

    public Map<Player, PokerRoundResult> calculateResults() {
        final HandRanking dealerRanking = dealer.getHandRanking();
        final Map<Player, PokerRoundResult> results = new HashMap<>();
        for (Map.Entry<Player, PlayerPokerHand> entry : playerHands.entrySet()) {
            results.put(entry.getKey(), entry.getValue().determineResult(dealerRanking));
        }
        return results;
    }

    public void markReady(Player player) {
        readyPlayers.add(player);
    }

    public boolean isAllReady(List<Player> players) {
        return readyPlayers.containsAll(players);
    }

    public Dealer getDealer() {
        return dealer;
    }

    public boolean isFirst() {
        return number == 1;
    }

    public boolean isLast() {
        return number == total;
    }

    public int getNumber() {
        return number;
    }

    private PlayerPokerHand findHand(Player player) {
        final PlayerPokerHand hand = playerHands.get(player);
        if (hand == null) {
            throw new BusinessException(
                    NumberPokerErrorCode.PLAYER_NOT_FOUND,
                    "해당 플레이어가 이 라운드에 없습니다. player=" + player.getName()
            );
        }
        return hand;
    }
}
