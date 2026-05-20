package coffeeshout.websocket;

import lombok.NonNull;

/**
 * 플레이어 식별 키 (joinCode:playerName)
 * <p>웹소켓 세션 매핑, Principal 설정 등에서 공통으로 사용</p>
 */
public record PlayerKey(@NonNull String joinCode, @NonNull String playerName) {

    private static final String DELIMITER = ":";

    public PlayerKey {
        if (joinCode.isEmpty() || playerName.isEmpty()) {
            throw new IllegalArgumentException("joinCode와 playerName은 비어있을 수 없습니다");
        }
        if (joinCode.contains(DELIMITER) || playerName.contains(DELIMITER)) {
            throw new IllegalArgumentException(
                    "joinCode와 playerName에 구분자('" + DELIMITER + "')가 포함될 수 없습니다");
        }
    }

    public static PlayerKey of(@NonNull String joinCode, @NonNull String playerName) {
        return new PlayerKey(joinCode, playerName);
    }

    public static PlayerKey parse(@NonNull String playerKey) {
        String[] parts = playerKey.split(DELIMITER);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new IllegalArgumentException(
                    "플레이어 키 형식이 잘못되었습니다. 예상: joinCode" + DELIMITER + "playerName, 실제: " + playerKey);
        }
        return new PlayerKey(parts[0], parts[1]);
    }

    public static String prefix(@NonNull String joinCode) {
        return joinCode + DELIMITER;
    }

    public static boolean isValid(String playerKey) {
        if (playerKey == null || !playerKey.contains(DELIMITER)) {
            return false;
        }
        String[] parts = playerKey.split(DELIMITER);
        return parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty();
    }

    @Override
    public String toString() {
        return joinCode + DELIMITER + playerName;
    }
}
