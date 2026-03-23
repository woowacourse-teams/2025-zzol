package coffeeshout.bombrelay.domain;

public record WordValidationResult(
        Status status,
        BombRelayGameErrorCode errorCode
) {

    public enum Status {
        ACCEPTED,
        REJECTED,
        NEEDS_DICTIONARY_CHECK
    }

    public static WordValidationResult rejected(BombRelayGameErrorCode errorCode) {
        return new WordValidationResult(Status.REJECTED, errorCode);
    }

    public static WordValidationResult needsDictionaryCheck() {
        return new WordValidationResult(Status.NEEDS_DICTIONARY_CHECK, null);
    }

    public static WordValidationResult accepted() {
        return new WordValidationResult(Status.ACCEPTED, null);
    }

    public boolean isRejected() {
        return status == Status.REJECTED;
    }

    public boolean requiresDictionaryCheck() {
        return status == Status.NEEDS_DICTIONARY_CHECK;
    }
}
