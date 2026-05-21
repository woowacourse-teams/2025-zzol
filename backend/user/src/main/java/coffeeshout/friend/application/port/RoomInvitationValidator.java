package coffeeshout.friend.application.port;

public interface RoomInvitationValidator {

    void validateRoomIsLobby(String joinCode);

    void validateInviterInRoom(String joinCode, Long inviterUserId);
}
