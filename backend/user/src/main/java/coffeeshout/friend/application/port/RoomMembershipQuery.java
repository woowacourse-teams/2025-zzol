package coffeeshout.friend.application.port;

import coffeeshout.friend.domain.RoomMembership;
import java.util.Collection;
import java.util.Map;

/**
 * 사용자가 현재 어느 방에 참여 중인지 조회하는 아웃바운드 포트.
 *
 * <p>{@code :user}가 {@code :room}을 몰라야 하므로 friend 모듈은 이 인터페이스만 알고,
 * 구현은 {@code :room}이 제공한다({@code RoomInvitationValidator}와 동일한 방향).
 */
public interface RoomMembershipQuery {

    /**
     * 방에 참여 중인 사용자만 결과에 담는다. 어느 방에도 없는 사용자는 키 자체가 없다.
     */
    Map<Long, RoomMembership> findByUserIds(Collection<Long> userIds);
}
