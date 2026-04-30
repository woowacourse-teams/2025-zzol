package coffeeshout.user.ui;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.application.service.UserProfileService;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.domain.User;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.ui.request.UpdateNicknameRequest;
import coffeeshout.user.ui.resolver.AuthUser;
import coffeeshout.user.ui.response.UserMeResponse;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserRestController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe(@AuthUser Optional<AuthenticatedUser> authUser) {
        final AuthenticatedUser user = requireAuthenticated(authUser);
        final User found = userProfileService.findById(user.userId());
        return ResponseEntity.ok(UserMeResponse.from(found));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<UserMeResponse> updateNickname(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        final AuthenticatedUser user = requireAuthenticated(authUser);
        final User updated = userProfileService.changeNickname(user.userId(), request.nickname());
        return ResponseEntity.ok(UserMeResponse.from(updated));
    }

    private AuthenticatedUser requireAuthenticated(Optional<AuthenticatedUser> authUser) {
        return authUser.orElseThrow(() -> new BusinessException(
                UserErrorCode.UNAUTHORIZED, "인증이 필요합니다."));
    }
}
