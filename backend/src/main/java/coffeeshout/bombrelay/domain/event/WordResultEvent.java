package coffeeshout.bombrelay.domain.event;

public record WordResultEvent(
        String joinCode,
        String playerName,
        String word,
        boolean accepted,
        String rejectReason
) {

    public static WordResultEvent accepted(String joinCode, String playerName, String word) {
        return new WordResultEvent(joinCode, playerName, word, true, null);
    }

    public static WordResultEvent rejected(String joinCode, String playerName, String word, String reason) {
        return new WordResultEvent(joinCode, playerName, word, false, reason);
    }
}
