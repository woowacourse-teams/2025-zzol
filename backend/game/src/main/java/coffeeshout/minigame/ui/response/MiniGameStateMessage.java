package coffeeshout.minigame.ui.response;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;

public record MiniGameStateMessage(
        String cardGameState,
        String currentRound,
        List<CardInfoMessage> cardInfoMessages,
        Boolean allSelected
) {

    private enum RoundLabel {
        READY(0),
        FIRST(1),
        SECOND(2),
        ;

        private final int index;

        RoundLabel(int index) {
            this.index = index;
        }

        static RoundLabel from(int roundIndex) {
            return Arrays.stream(values())
                    .filter(label -> label.index == roundIndex)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 라운드 인덱스: " + roundIndex));
        }
    }

    public record CardInfoMessage(
            String cardType,
            int value,
            boolean selected,
            String playerName,
            Integer colorIndex
    ) {

        public static List<CardInfoMessage> from(@NonNull CardGame cardGame) {
            return cardGame.getDeck().getCards().stream()
                    .map(card -> {
                        Optional<Player> player = cardGame.findCardOwnerInCurrentRound(card);
                        PlayerName name = player.map(Player::getName).orElse(null);
                        Integer colorIndex = player.map(Player::getColorIndex).orElse(null);
                        return CardInfoMessage.of(card, player.isPresent(), name, colorIndex);
                    }).toList();
        }

        public static CardInfoMessage of(@NonNull Card card, boolean isSelected, PlayerName name, Integer colorIndex) {
            return new CardInfoMessage(
                    card.getType().name(),
                    card.getValue(),
                    isSelected,
                    name == null ? null : name.value(),
                    colorIndex
            );
        }
    }

    public static MiniGameStateMessage from(@NonNull CardGame cardGame) {
        return new MiniGameStateMessage(
                cardGame.getState().name(),
                RoundLabel.from(cardGame.getRound().toIndex()).name(),
                CardInfoMessage.from(cardGame),
                cardGame.isFinishedThisRound()
        );
    }
}
