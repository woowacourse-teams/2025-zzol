package coffeeshout.room.ui.response;

public record JoinCodeExistResponse(
        boolean exist
) {

    public static JoinCodeExistResponse from(boolean existence) {
        return new JoinCodeExistResponse(existence);
    }
}
