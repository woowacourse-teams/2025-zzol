package coffeeshout.cardgame.domain.event.dto;

public record CardGameStartMessage(
        String joinCode,
        String cardGameTaskType
) {
}
