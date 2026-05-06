package coffeeshout.friend.ui;

import coffeeshout.friend.application.service.RoomInvitationService;
import coffeeshout.friend.ui.request.SendRoomInvitationRequest;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.ui.resolver.AuthUser;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomInvitationRestController {

    private final RoomInvitationService roomInvitationService;

    @PostMapping("/{joinCode}/invitations")
    public ResponseEntity<Void> invite(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @PathVariable String joinCode,
            @Valid @RequestBody SendRoomInvitationRequest request) {
        final AuthenticatedUser me = requireAuthenticated(authUser);
        roomInvitationService.invite(me.userId(), request.targetUserId(), joinCode);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUser requireAuthenticated(Optional<AuthenticatedUser> authUser) {
        return authUser.orElseThrow(() -> new BusinessException(
                UserErrorCode.UNAUTHORIZED, "인증이 필요합니다."));
    }
}
