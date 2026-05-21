package coffeeshout.websocket;

import coffeeshout.exception.custom.BusinessException;
import java.security.Principal;
import lombok.NonNull;

/**
 * 플레이어 식별 키 (joinCode:playerName[:userId])
 * <p>웹소켓 세션 매핑, Principal 설정 등에서 공통으로 사용</p>
 * <p>로그인 사용자는 userId를 포함해 Principal을 구성한다.</p>
 */
public record PlayerKey(@NonNull String joinCode, @NonNull String playerName, Long userId) {

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
        return new PlayerKey(joinCode, playerName, null);
    }

    public static PlayerKey of(@NonNull String joinCode, @NonNull String playerName, Long userId) {
        return new PlayerKey(joinCode, playerName, userId);
    }

    public static PlayerKey parse(@NonNull String playerKey) {
        final String[] parts = playerKey.split(DELIMITER, -1);
        if (parts.length == 2) {
            if (parts[0].isEmpty() || parts[1].isEmpty()) {
                throw new BusinessException(PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT,
                        "플레이어 키 형식이 잘못되었습니다: " + playerKey);
            }
            return new PlayerKey(parts[0], parts[1], null);
        }
        if (parts.length == 3) {
            if (parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
                throw new BusinessException(PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT,
                        "플레이어 키 형식이 잘못되었습니다: " + playerKey);
            }
            try {
                return new PlayerKey(parts[0], parts[1], Long.parseLong(parts[2]));
            } catch (NumberFormatException e) {
                throw new BusinessException(PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT,
                        "플레이어 키 형식이 잘못되었습니다. userId는 숫자여야 합니다: " + playerKey);
            }
        }
        throw new BusinessException(PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT,
                "플레이어 키 형식이 잘못되었습니다: " + playerKey);
    }

    public static String prefix(@NonNull String joinCode) {
        return joinCode + DELIMITER;
    }

    public static boolean isValid(String playerKey) {
        if (playerKey == null) {
            return false;
        }
        try {
            parse(playerKey);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        if (userId != null) {
            return joinCode + DELIMITER + playerName + DELIMITER + userId;
        }
        return joinCode + DELIMITER + playerName;
    }
}
