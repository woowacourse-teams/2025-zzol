package coffeeshout.room.application.service;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 친구 알림에 필요한 방 상태만 추린 값. 변이 <b>직전</b>과 <b>직후</b> 두 개를 비교해 누구에게 무엇이 바뀌었는지
 * 판단한다({@link RoomPresencePublisher}).
 *
 * <p>직전 상태를 방 밖에 오래 보관하지 않는 이유는 {@code RoomCommandService.mutate()}에 적혀 있다.
 */
public record RoomPresence(String joinCode, Set<Long> userIds, boolean joinable) {

    public static RoomPresence of(Room room) {
        // 방을 소유하지 않은 스레드에서 훑는다 — 순회 중 입퇴장이 겹쳐도 안전한 이유는 Players 생성자에 있다.
        final Set<Long> userIds = room.getPlayers().stream()
                .map(Player::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        return new RoomPresence(room.getJoinCode().getValue(), userIds, room.isJoinable());
    }

    /**
     * 아직 만들어지지 않았거나 이미 삭제된 방. 생성 직전·삭제 직후의 비교 대상으로 쓴다.
     */
    public static RoomPresence empty(JoinCode joinCode) {
        return new RoomPresence(joinCode.getValue(), Set.of(), false);
    }
}
