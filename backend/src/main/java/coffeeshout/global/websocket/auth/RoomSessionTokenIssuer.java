package coffeeshout.global.websocket.auth;

public interface RoomSessionTokenIssuer {

    String issue(RoomSessionClaim claim);

    RoomSessionClaim verify(String token);
}
