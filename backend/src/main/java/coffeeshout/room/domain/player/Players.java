package coffeeshout.room.domain.player;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.roulette.Probability;
import coffeeshout.room.domain.roulette.ProbabilityCalculator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.Getter;

@Getter
public class Players {

    private static final Random RANDOM = new Random();

    private final List<Player> players;
    private final ColorUsage colorUsage;

    public Players(String joinCode) {
        this.players = Collections.synchronizedList(new ArrayList<>());
        this.colorUsage = new ColorUsage(joinCode);
    }

    public synchronized Player join(Player player) {
        player.assignColorIndex(colorUsage.pickRandomOne());
        player.updateProbability(Probability.ZERO);
        this.players.add(player);
        adjustInitialPlayerProbabilities();
        return getPlayer(player.getName());
    }

    public void adjustProbabilities(MiniGameResult miniGameResult, ProbabilityCalculator probabilityCalculator) {
        for (Player player : players) {
            final int rank = miniGameResult.getPlayerRank(player);
            final int probabilityChange = probabilityCalculator.calculateProbabilityChange(
                    rank,
                    miniGameResult.getTieCountByRank(rank)
            );
            final Probability adjustedProbability = player.getProbability().plus(probabilityChange);
            player.updateProbability(adjustedProbability);
        }
    }

    public boolean hasEnoughPlayers(int minimumGuestCount, int maximumGuestCount) {
        return players.size() >= minimumGuestCount && players.size() <= maximumGuestCount;
    }

    public Player getPlayer(PlayerName playerName) {
        return players.stream()
                .filter(p -> p.sameName(playerName))
                .findFirst()
                .orElseThrow(() -> new InvalidArgumentException(
                        RoomErrorCode.NO_EXIST_PLAYER,
                        "사용자가 존재하지 않습니다. name = " + playerName.value()
                ));
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean hasDuplicateName(PlayerName playerName) {
        return players.stream().anyMatch(player -> player.sameName(playerName));
    }

    public boolean isAllReady() {
        return players.stream()
                .allMatch(Player::getIsReady);
    }

    public synchronized boolean removePlayer(PlayerName playerName) {
        return players.removeIf(player -> {
            if (player.sameName(playerName)) {
                colorUsage.release(player.getColorIndex());
                adjustInitialPlayerProbabilities();
                return true;
            }
            return false;
        });
    }

    public boolean existsByName(PlayerName playerName) {
        return players.stream()
                .anyMatch(player -> player.sameName(playerName));
    }

    public Player getFirstPlayer() {
        if (players.isEmpty()) {
            throw new InvalidArgumentException(
                    RoomErrorCode.NO_EXIST_PLAYER,
                    "플레이어가 존재하지 않습니다."
            );
        }
        return players.getFirst();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    private void adjustInitialPlayerProbabilities() {
        final Probability probability = Probability.TOTAL.divide(players.size());
        for (Player p : players) {
            p.updateProbability(probability);
        }
    }
}
