package coffeeshout.cardgame.domain;

import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.room.domain.RoomErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlayerHands {

    private final Map<Gamer, CardHand> playerHands;

    public PlayerHands(List<Gamer> gamers) {
        this.playerHands = gamers.stream().collect(Collectors.toMap(
                gamer -> gamer,
                gamer -> new CardHand()
        ));
    }

    public void putByGamer(Gamer gamer, Card card) {
        final CardHand hand = playerHands.get(gamer);
        if (hand == null) {
            throw new BusinessException(
                    RoomErrorCode.NO_EXIST_PLAYER,
                    "해당 플레이어를 찾을 수 없습니다. name: " + gamer.name()
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

    public boolean containsGamer(Gamer gamer) {
        return playerHands.containsKey(gamer);
    }

    public Map<Gamer, MiniGameScore> scoreByGamer() {
        return playerHands.entrySet().stream().collect(Collectors.toMap(
                Entry::getKey,
                entry -> entry.getValue().calculateCardGameScore()
        ));
    }

    public List<Gamer> getUnselectedGamers(CardGameRound round) {
        final List<Gamer> unselected = new ArrayList<>();
        playerHands.forEach((gamer, hand) -> {
            if (!hand.isSelected(round)) {
                unselected.add(gamer);
            }
        });
        return unselected;
    }

    public Optional<Gamer> findCardOwner(Card card, CardGameRound round) {
        return playerHands.entrySet().stream()
                .filter(entry -> entry.getValue().isAssign(card, round))
                .findFirst()
                .map(Entry::getKey);
    }
}
