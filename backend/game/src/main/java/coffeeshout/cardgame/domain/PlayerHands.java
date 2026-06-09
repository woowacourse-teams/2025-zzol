package coffeeshout.cardgame.domain;

import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.global.exception.custom.BusinessException;
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

    public void put(Gamer gamer, Card card) {
        playerHands.get(gamer).put(card);
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

    public Gamer findByName(String name) {
        return playerHands.keySet().stream()
                .filter(gamer -> gamer.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        RoomErrorCode.NO_EXIST_PLAYER,
                        "해당 플레이어를 찾을 수 없습니다. name: " + name)
                );
    }

    public Map<Gamer, MiniGameScore> scoreByPlayer() {
        return playerHands.entrySet().stream().collect(Collectors.toMap(
                Entry::getKey,
                entry -> entry.getValue().calculateCardGameScore()
        ));
    }

    public List<Gamer> getUnselectedPlayers(CardGameRound round) {
        final List<Gamer> gamers = new ArrayList<>();
        playerHands.forEach((gamer, hand) -> {
            if (!hand.isSelected(round)) {
                gamers.add(gamer);
            }
        });
        return gamers;
    }

    public Optional<Gamer> findCardOwner(Card card, CardGameRound round) {
        return playerHands.entrySet().stream()
                .filter(entry -> entry.getValue().isAssign(card, round))
                .findFirst()
                .map(Entry::getKey);
    }
}
