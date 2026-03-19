package coffeeshout.bombrelay.ui.response;

import coffeeshout.bombrelay.domain.event.WordResultEvent;

public record WordResultResponse(
        String playerName,
        String word,
        boolean accepted,
        String rejectReason
) {

    public static WordResultResponse from(WordResultEvent event) {
        return new WordResultResponse(
                event.playerName(),
                event.word(),
                event.accepted(),
                event.rejectReason()
        );
    }
}
