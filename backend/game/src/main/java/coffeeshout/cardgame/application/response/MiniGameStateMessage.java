package coffeeshout.cardgame.application.response;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.CardGameState;
import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.CardType;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.SystemException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

public record MiniGameStateMessage(
        CardGameState cardGameState,
        RoundLabel currentRound,
        List<CardInfoMessage> cardInfoMessages,
        Boolean allSelected) {

    public enum RoundLabel {
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
                    .orElseThrow(() -> new SystemException(
                            GlobalErrorCode.INTERNAL_SERVER_ERROR, "지원하지 않는 라운드 인덱스: " + roundIndex));
        }
    }

    public record CardInfoMessage(
            CardType cardType,
            int value,
            boolean selected,
            @Nullable String playerName,
            @Nullable Integer colorIndex) {

        public static List<CardInfoMessage> from(@NonNull CardGame cardGame) {
            return cardGame.getDeck().getCards().stream()
                    .map(card -> {
                        final Optional<Gamer> owner = cardGame.findCardOwnerInCurrentRound(card);
                        final String name = owner.map(Gamer::getName).orElse(null);
                        final Integer colorIndex =
                                owner.map(Gamer::getColorIndex).orElse(null);
                        return CardInfoMessage.of(card, owner.isPresent(), name, colorIndex);
                    })
                    .toList();
        }

        public static CardInfoMessage of(@NonNull Card card, boolean isSelected, String name, Integer colorIndex) {
            return new CardInfoMessage(card.getType(), card.getValue(), isSelected, name, colorIndex);
        }
    }

    public static MiniGameStateMessage from(@NonNull CardGame cardGame) {
        return new MiniGameStateMessage(
                cardGame.getState(),
                RoundLabel.from(cardGame.getRound().toIndex()),
                CardInfoMessage.from(cardGame),
                cardGame.isFinishedThisRound());
    }
}
