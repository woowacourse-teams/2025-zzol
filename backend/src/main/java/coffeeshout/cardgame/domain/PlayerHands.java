package coffeeshout.cardgame.domain;

import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlayerHands {

    private final Map<Player, CardHand> playerHands;

    public PlayerHands(List<Player> players) {
        this.playerHands = players.stream().collect(Collectors.toMap(
                player -> player,
                player -> new CardHand()
        ));
    }

    public void put(Player player, Card card) {
        playerHands.get(player).put(card);
    }

    public int totalHandSize() {
        return playerHands.values().stream()
                .mapToInt(CardHand::size)
                .sum();
    }

    public int playerCount() {
        return playerHands.size();
    }

    public boolean isRoundFinished() {
        if (totalHandSize() == 0) {
            return false;
        }

        return totalHandSize() % playerCount() == 0;
    }

    public Player findPlayerByName(PlayerName name) {
        return playerHands.keySet().stream()
                .filter(player -> player.sameName(name))
                .findFirst()
                .orElseThrow(() -> new InvalidArgumentException(
                        RoomErrorCode.NO_EXIST_PLAYER,
                        "해당 플레이어를 찾을 수 없습니다. name: " + name)
                );
    }

    public Map<Player, MiniGameScore> scoreByPlayer() {
        return playerHands.entrySet().stream().collect(Collectors.toMap(
                Entry::getKey,
                entry -> entry.getValue().calculateCardGameScore()
        ));
    }

    public List<Player> getUnselectedPlayers(CardGameRound round) {
        final List<Player> players = new ArrayList<>();
        playerHands.forEach((player, hand) -> {
            if (!hand.isSelected(round)) {
                players.add(player);
            }
        });
        return players;
    }

    public Optional<Player> findCardOwner(Card card, CardGameRound round) {
        return playerHands.entrySet().stream()
                .filter(entry -> entry.getValue().isAssign(card, round))
                .findFirst()
                .map(Entry::getKey);
    }
}
