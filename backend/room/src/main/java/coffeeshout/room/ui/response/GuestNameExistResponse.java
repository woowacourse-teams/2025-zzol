package coffeeshout.room.ui.response;

public record GuestNameExistResponse(
        boolean exist
) {

    public static GuestNameExistResponse from(boolean existence) {
        return new GuestNameExistResponse(existence);
    }
}
