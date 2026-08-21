package coffeeshout.friend.domain;

/**
 * 사용자가 현재 참여 중인 방과, 그 방에 다른 사람이 입장할 수 있는지를 담는다.
 *
 * <p>{@code joinCode} 하나로는 "방에 없음 / 참여 가능 / 참여 불가(게임 중·정원참)"의 세 상태를 구분할 수 없어
 * {@code joinable}을 함께 둔다. 친구 목록의 {@code 참여하기} 노출과 초대 대상 검증이 서로 다른 조건을 쓴다.
 */
public record RoomMembership(String joinCode, boolean joinable) {

    /**
     * 어느 방에도 없는 상태. 조회 결과에 없는 사용자를 이 값으로 채우면 호출부에 null 분기가 생기지 않는다.
     */
    public static final RoomMembership NONE = new RoomMembership(null, false);
}
