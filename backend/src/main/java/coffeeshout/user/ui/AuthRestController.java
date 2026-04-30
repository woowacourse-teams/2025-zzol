package coffeeshout.user.ui;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.application.service.AuthTokenService.TokenPair;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.ui.resolver.AuthUser;
import coffeeshout.user.ui.response.AuthTokenResponse;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthTokenService authTokenService;

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@RequestBody Map<String, String> body) {
        final String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(UserErrorCode.REFRESH_TOKEN_NOT_FOUND, "리프레시 토큰이 없습니다.");
        }
        final TokenPair tokens = authTokenService.rotate(refreshToken);
        return ResponseEntity.ok(new AuthTokenResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthUser Optional<AuthenticatedUser> authUser) {
        authUser.ifPresent(user -> authTokenService.revoke(user.userId()));
        return ResponseEntity.noContent().build();
    }
}
