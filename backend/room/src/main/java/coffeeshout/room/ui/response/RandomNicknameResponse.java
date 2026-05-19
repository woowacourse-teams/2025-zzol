package coffeeshout.room.ui.response;

public record RandomNicknameResponse(String nickname) {

    public static RandomNicknameResponse from(String nickname) {
        return new RandomNicknameResponse(nickname);
    }
}
