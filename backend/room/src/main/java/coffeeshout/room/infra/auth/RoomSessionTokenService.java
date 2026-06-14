package coffeeshout.room.infra.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomSessionTokenService {

    private final RoomSessionTokenIssuer issuer;

    public String issue(String joinCode, String playerName, Long userId) {
        final RoomSessionClaim claim = RoomSessionClaim.of(joinCode, playerName, userId);
        return issuer.issue(claim);
    }

    public RoomSessionClaim verify(String token) {
        return issuer.verify(token);
    }
}
