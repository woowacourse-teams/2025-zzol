package coffeeshout.room.ui;

import coffeeshout.room.application.service.RoomInvitationService;
import coffeeshout.room.ui.request.SendRoomInvitationRequest;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.ui.resolver.AuthUser;
import jakarta.validation.Valid;
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
            @AuthUser AuthenticatedUser me,
            @PathVariable String joinCode,
            @Valid @RequestBody SendRoomInvitationRequest request) {
        roomInvitationService.invite(me.userId(), request.targetUserId(), joinCode);
        return ResponseEntity.noContent().build();
    }
}
