package coffeeshout.room.infra.auth;

public interface RoomSessionTokenIssuer {

    String issue(RoomSessionClaim claim);

    RoomSessionClaim verify(String token);
}
