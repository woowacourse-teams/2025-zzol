package coffeeshout.cardgame.domain;

import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.exception.custom.BusinessException;
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

    private final Map<PlayerName, CardHand> playerHands;

    public PlayerHands(List<PlayerName> playerNames) {
        this.playerHands = playerNames.stream().collect(Collectors.toMap(
                playerName -> playerName,
                playerName -> new CardHand()
        ));
    }

    public void put(Player player, Card card) {
        putByName(player.getName(), card);
    }

    public void putByName(PlayerName playerName, Card card) {
        final CardHand hand = playerHands.get(playerName);
        if (hand == null) {
            throw new BusinessException(
                    RoomErrorCode.NO_EXIST_PLAYER,
                    "해당 플레이어를 찾을 수 없습니다. name: " + playerName
            );
        }
        hand.put(card);
    }

    public int totalHandSize() {
        return playerHands.values().stream()
                .mapToInt(CardHand::size)
                .sum();
    }

    public int playerCount() {
        return playerHands.size();
    }

    public boolean isRoundFinished(CardGameRound round) {
        return playerHands.values().stream()
                .allMatch(hand -> hand.isSelected(round));
    }

    public boolean containsPlayer(PlayerName name) {
        return playerHands.containsKey(name);
    }

    public Map<PlayerName, MiniGameScore> scoreByPlayer() {
        return playerHands.entrySet().stream().collect(Collectors.toMap(
                Entry::getKey,
                entry -> entry.getValue().calculateCardGameScore()
        ));
    }

    public List<PlayerName> getUnselectedPlayerNames(CardGameRound round) {
        final List<PlayerName> names = new ArrayList<>();
        playerHands.forEach((name, hand) -> {
            if (!hand.isSelected(round)) {
                names.add(name);
            }
        });
        return names;
    }

    public Optional<PlayerName> findCardOwner(Card card, CardGameRound round) {
        return playerHands.entrySet().stream()
                .filter(entry -> entry.getValue().isAssign(card, round))
                .findFirst()
                .map(Entry::getKey);
    }
}
