package coffeeshout.friend.application.dto;

public record FriendResponsePayload(Long requestId, boolean accepted, Long counterpartUserId,
                                    String counterpartUserCode, String counterpartNickname) {
}
