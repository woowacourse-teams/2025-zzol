package coffeeshout.admin.auth.application;

import coffeeshout.admin.account.application.AdminAccountService;
import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.AdminAuthProperties;
import coffeeshout.admin.auth.domain.AdminRefreshToken;
import coffeeshout.admin.auth.domain.AdminRefreshTokenRepository;
import coffeeshout.admin.auth.domain.AdminTokenIssuer;
import coffeeshout.admin.auth.domain.SocialIdTokenVerifier;
import coffeeshout.global.exception.custom.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 관리자 로그인과 재발급.
 *
 * <p>순서가 중요하다. <b>토큰 검증 → 허용목록 대조 → 토큰 발급</b>이다.
 * 허용목록을 먼저 보면 이메일을 주장하기만 해도 목록에 있는지 알아낼 수 있어,
 * 관리자 이메일을 찾는 탐색 경로가 열린다.
 *
 * <p>재발급 때도 허용목록을 다시 본다. access 토큰 검증은 서명과 만료만 보므로, 목록에서 뺀
 * 관리자를 실제로 막는 자리는 여기다. 막히기까지 최대 access 수명(1시간)이 걸린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final SocialIdTokenVerifier socialIdTokenVerifier;
    private final AdminAccountService adminAccountService;
    private final AdminTokenIssuer adminTokenIssuer;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final AdminAuthProperties adminAuthProperties;

    public AdminTokens login(String idToken) {
        final AdminEmail email = allowedEmail(socialIdTokenVerifier.verifyAndExtractEmail(idToken));
        final AdminRefreshToken refreshToken = AdminRefreshToken.newFamily();
        adminRefreshTokenRepository.save(refreshToken, email, adminAuthProperties.refreshTokenValidity());
        return new AdminTokens(adminTokenIssuer.issue(email), refreshToken);
    }

    public AdminTokens refresh(String rawRefreshToken) {
        final AdminRefreshToken presented =
                AdminRefreshToken.parse(rawRefreshToken).orElseThrow(AdminAuthService::invalidRefreshToken);
        final AdminRefreshToken next = presented.rotate();
        final AdminEmail email = adminRefreshTokenRepository
                .rotate(presented, next, adminAuthProperties.refreshTokenValidity())
                .orElseThrow(AdminAuthService::invalidRefreshToken);

        if (!adminAccountService.isAllowed(email)) {
            adminRefreshTokenRepository.revoke(presented.familyId());
            throw new BusinessException(AdminAccountErrorCode.NOT_ADMIN, "관리자 허용목록에 없는 계정입니다.");
        }
        return new AdminTokens(adminTokenIssuer.issue(email), next);
    }

    /**
     * 쿠키가 없거나 망가졌어도 실패하지 않는다. 로그아웃은 "이 브라우저를 로그아웃 상태로 만든다"가
     * 목적이라 이미 그 상태면 할 일이 없을 뿐이다.
     */
    public void logout(String rawRefreshToken) {
        AdminRefreshToken.parse(rawRefreshToken)
                .ifPresent(token -> adminRefreshTokenRepository.revoke(token.familyId()));
    }

    private AdminEmail allowedEmail(String rawEmail) {
        // 형식이 어긋난 값도 목록에 없는 값과 같은 응답을 준다. 둘을 구분해 주면
        // 어떤 이메일이 형식만 맞는지 훑어 관리자 계정을 좁혀 갈 수 있다.
        return AdminEmail.parse(rawEmail)
                .filter(adminAccountService::isAllowed)
                .orElseThrow(() -> new BusinessException(AdminAccountErrorCode.NOT_ADMIN, "관리자 허용목록에 없는 계정입니다."));
    }

    private static BusinessException invalidRefreshToken() {
        return new BusinessException(AdminAccountErrorCode.ADMIN_REFRESH_TOKEN_INVALID, "유효하지 않은 관리자 refresh 토큰입니다.");
    }
}
