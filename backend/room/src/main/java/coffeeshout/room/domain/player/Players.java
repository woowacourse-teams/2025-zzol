package coffeeshout.room.domain.player;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.roulette.Probability;
import coffeeshout.room.domain.roulette.ProbabilityCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class Players {

    private final List<Player> players;
    private final ColorUsage colorUsage;

    public Players(String joinCode) {
        this.players = new ArrayList<>();
        this.colorUsage = new ColorUsage(joinCode);
    }

    public Player join(Player player) {
        player.assignColorIndex(colorUsage.pickRandomOne());
        player.updateProbability(Probability.ZERO);
        this.players.add(player);
        adjustInitialPlayerProbabilities();
        return getPlayer(player.getName());
    }

    /**
     * 순위 맵(이름 기준)으로 확률을 조정한다. 게임 결과가 {@code MiniGameFinishedEvent}로 전달되는
     * 경로(ADR-0023 결정 5)에서 사용한다. 동점 수는 {@code MiniGameResult.getTieCountByRank}와 동일하게
     * "같은 순위를 가진 플레이어 수"로 계산한다.
     */
    public void adjustProbabilities(Map<PlayerName, Integer> rankByPlayer,
                                    ProbabilityCalculator probabilityCalculator) {
        for (Player player : players) {
            final int rank = rankByPlayer.get(player.getName());
            final int tieCount = (int) rankByPlayer.values().stream()
                    .filter(value -> value == rank)
                    .count();
            final int probabilityChange = probabilityCalculator.calculateProbabilityChange(rank, tieCount);
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
                .orElseThrow(() -> new BusinessException(
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

    public boolean removePlayer(PlayerName playerName) {
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
            throw new BusinessException(
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
