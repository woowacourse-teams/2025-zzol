package coffeeshout.user.ui;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.user.application.service.TermsService;
import coffeeshout.user.application.service.UserProfileService;
import coffeeshout.user.application.service.UserStatsService;
import coffeeshout.user.application.service.UserWithdrawalService;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserStats;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.ui.request.UpdateNicknameRequest;
import coffeeshout.user.ui.request.UpdateStatsRequest;
import coffeeshout.user.ui.resolver.AuthUser;
import coffeeshout.user.ui.response.UserMeResponse;
import coffeeshout.user.ui.response.UserStatsResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserRestController {

    private final UserProfileService userProfileService;
    private final UserStatsService userStatsService;
    private final TermsService termsService;
    private final UserWithdrawalService userWithdrawalService;
    private final RefreshTokenCookieHelper cookieHelper;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe(@AuthUser Optional<AuthenticatedUser> authUser) {
        final AuthenticatedUser user = requireAuthenticated(authUser);
        final User found = userProfileService.findById(user.userId());
        return ResponseEntity.ok(UserMeResponse.from(found));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserMeResponse> updateMe(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        final AuthenticatedUser user = requireAuthenticated(authUser);
        final User updated = userProfileService.changeNickname(user.userId(), request.nickname());
        return ResponseEntity.ok(UserMeResponse.from(updated));
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

    @GetMapping("/me/stats")
    public ResponseEntity<UserStatsResponse> getStats(@AuthUser Optional<AuthenticatedUser> authUser) {
        final AuthenticatedUser user = requireAuthenticated(authUser);
        final UserStats stats = userStatsService.getStats(user.userId());
        return ResponseEntity.ok(UserStatsResponse.from(stats));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            @AuthUser Optional<AuthenticatedUser> authUser,
            HttpServletResponse response
    ) {
        final AuthenticatedUser user = requireAuthenticated(authUser);
        userWithdrawalService.withdraw(user.userId());
        cookieHelper.clear(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/terms")
    public ResponseEntity<Void> agreeTerms(@AuthUser Optional<AuthenticatedUser> authUser) {
        final AuthenticatedUser user = requireAuthenticated(authUser);
        termsService.agreeTerms(user.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/stats")
    public ResponseEntity<UserStatsResponse> updateStats(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @Valid @RequestBody UpdateStatsRequest request
    ) {
        final AuthenticatedUser user = requireAuthenticated(authUser);
        final UserStats updated = userStatsService.updateStats(user.userId(), request.isWinner());
        return ResponseEntity.ok(UserStatsResponse.from(updated));
    }

    private AuthenticatedUser requireAuthenticated(Optional<AuthenticatedUser> authUser) {
        return authUser.orElseThrow(() -> new BusinessException(
                UserErrorCode.UNAUTHORIZED, "인증이 필요합니다."));
    }
}
