package coffeeshout.admin.room.domain;

import coffeeshout.room.domain.player.PlayerType;
import java.time.LocalDateTime;

/**
 * @param userId   로그인 사용자면 값이 있고 게스트면 null 이다.
 * @param nickname 로그인 사용자의 계정 닉네임. 방에서 쓴 이름(playerName)과 다를 수 있다.
 */
public record RoomPlayer(
        Long id,
        String playerName,
        PlayerType playerType,
        Long userId,
        String nickname,
        String userCode,
        LocalDateTime joinedAt) {}
