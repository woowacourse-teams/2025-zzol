package coffeeshout.global.websocket.auth;

public record RoomSessionClaim(String joinCode, String playerName, Long userId) {

    public RoomSessionClaim {
        if (joinCode == null || joinCode.isBlank()) {
            throw new IllegalArgumentException("joinCode는 비어있을 수 없습니다.");
        }
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("playerName은 비어있을 수 없습니다.");
        }
    }

    public static RoomSessionClaim of(String joinCode, String playerName, Long userId) {
        return new RoomSessionClaim(joinCode, playerName, userId);
    }

    public static RoomSessionClaim ofAnonymous(String joinCode, String playerName) {
        return new RoomSessionClaim(joinCode, playerName, null);
    }
}
