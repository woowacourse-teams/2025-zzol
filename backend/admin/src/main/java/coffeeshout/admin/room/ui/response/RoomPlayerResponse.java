package coffeeshout.admin.room.ui.response;

import coffeeshout.admin.room.domain.RoomPlayer;
import coffeeshout.room.domain.player.PlayerType;
import java.time.LocalDateTime;

/**
 * @param guest 로그인하지 않은 참여자. 화면이 userId 의 null 여부를 따지지 않아도 되게 서버가 정한다.
 */
public record RoomPlayerResponse(
        Long id,
        String playerName,
        PlayerType playerType,
        boolean guest,
        Long userId,
        String nickname,
        String userCode,
        LocalDateTime joinedAt) {

    public static RoomPlayerResponse from(RoomPlayer player) {
        return new RoomPlayerResponse(
                player.id(),
                player.playerName(),
                player.playerType(),
                player.userId() == null,
                player.userId(),
                player.nickname(),
                player.userCode(),
                player.joinedAt());
    }
}
