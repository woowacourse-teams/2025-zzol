package coffeeshout.friend.ui.response;

import coffeeshout.user.domain.User;

public record AcceptFriendResponse(Long friendUserId, String friendUserCode, String friendNickname) {

    public static AcceptFriendResponse from(User user) {
        return new AcceptFriendResponse(
                user.getId(),
                user.getUserCode().value(),
                user.getNickname().value()
        );
    }
}
