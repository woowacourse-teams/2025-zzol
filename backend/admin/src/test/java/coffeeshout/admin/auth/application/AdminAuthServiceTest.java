package coffeeshout.admin.auth.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.admin.account.application.AdminAccountService;
import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.AdminAuthProperties;
import coffeeshout.admin.auth.domain.AdminRefreshToken;
import coffeeshout.admin.auth.domain.AdminRefreshTokenRepository;
import coffeeshout.admin.auth.domain.AdminTokenIssuer;
import coffeeshout.admin.auth.domain.SocialIdTokenVerifier;
import coffeeshout.global.exception.custom.BusinessException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AdminAuthService")
@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    private static final String ID_TOKEN = "google-id-token";
    private static final AdminEmail MJ = AdminEmail.of("mj@zzol.site");
    private static final AdminEmail STRANGER = AdminEmail.of("stranger@evil.site");
    private static final long REFRESH_SECONDS = 604800;
    private static final Duration REFRESH_TTL = Duration.ofSeconds(REFRESH_SECONDS);

    @Mock
    private SocialIdTokenVerifier socialIdTokenVerifier;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminTokenIssuer adminTokenIssuer;

    @Mock
    private AdminRefreshTokenRepository adminRefreshTokenRepository;

    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        final AdminAuthProperties properties = new AdminAuthProperties(
                List.of(),
                "client-id",
                "admin-test-secret-key-must-be-at-least-32-bytes-long",
                3600,
                REFRESH_SECONDS,
                List.of("http://localhost:5173"));
        adminAuthService = new AdminAuthService(
                socialIdTokenVerifier, adminAccountService, adminTokenIssuer, adminRefreshTokenRepository, properties);
    }

    @Nested
    class login {

        @Test
        void 허용목록에_있으면_관리자_토큰과_refresh를_발급한다() {
            given(socialIdTokenVerifier.verifyAndExtractEmail(ID_TOKEN)).willReturn("mj@zzol.site");
            given(adminAccountService.isAllowed(MJ)).willReturn(true);
            given(adminTokenIssuer.issue(MJ)).willReturn("admin-token");

            final AdminTokens tokens = adminAuthService.login(ID_TOKEN);

            final ArgumentCaptor<AdminRefreshToken> saved = ArgumentCaptor.forClass(AdminRefreshToken.class);
            then(adminRefreshTokenRepository).should().save(saved.capture(), eq(MJ), eq(REFRESH_TTL));
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tokens.accessToken()).isEqualTo("admin-token");
                softly.assertThat(tokens.refreshToken()).isEqualTo(saved.getValue());
            });
        }

        @Test
        void 구글이_준_이메일을_정규화해_판정하고_발급한다() {
            given(socialIdTokenVerifier.verifyAndExtractEmail(ID_TOKEN)).willReturn("  MJ@Zzol.Site ");
            given(adminAccountService.isAllowed(MJ)).willReturn(true);
            given(adminTokenIssuer.issue(MJ)).willReturn("admin-token");

            assertThat(adminAuthService.login(ID_TOKEN).accessToken()).isEqualTo("admin-token");
        }

        @Test
        void 허용목록에_없으면_토큰을_발급하지_않는다() {
            given(socialIdTokenVerifier.verifyAndExtractEmail(ID_TOKEN)).willReturn("stranger@evil.site");
            given(adminAccountService.isAllowed(STRANGER)).willReturn(false);

            assertCoffeeShoutException(() -> adminAuthService.login(ID_TOKEN), AdminAccountErrorCode.NOT_ADMIN);
            then(adminTokenIssuer).should(never()).issue(any());
            then(adminRefreshTokenRepository).should(never()).save(any(), any(), any());
        }

        @Test
        void 형식이_어긋난_이메일도_허용목록에_없는_것과_같은_응답을_준다() {
            // 둘을 구분해 주면 어떤 이메일이 형식만 맞는지 훑어 관리자 계정을 좁혀 갈 수 있다.
            given(socialIdTokenVerifier.verifyAndExtractEmail(ID_TOKEN)).willReturn("형식오류");

            assertCoffeeShoutException(() -> adminAuthService.login(ID_TOKEN), AdminAccountErrorCode.NOT_ADMIN);
            then(adminAccountService).should(never()).isAllowed(any());
            then(adminTokenIssuer).should(never()).issue(any());
        }

        @Test
        void 구글_검증이_실패하면_허용목록을_보지_않는다() {
            // 순서가 뒤바뀌면 이메일을 주장하는 것만으로 목록 등재 여부를 알아낼 수 있다.
            given(socialIdTokenVerifier.verifyAndExtractEmail(ID_TOKEN))
                    .willThrow(new BusinessException(AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID, "구글 인증에 실패했습니다."));

            assertCoffeeShoutException(
                    () -> adminAuthService.login(ID_TOKEN), AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID);
            then(adminAccountService).should(never()).isAllowed(any());
            then(adminTokenIssuer).should(never()).issue(any());
        }
    }

    @Nested
    class refresh {

        private final AdminRefreshToken presented = AdminRefreshToken.newFamily();

        @Test
        void 회전에_성공하고_허용목록에_있으면_새_토큰을_준다() {
            given(adminRefreshTokenRepository.findEmail(presented)).willReturn(Optional.of(MJ));
            given(adminRefreshTokenRepository.rotate(eq(presented), any(), eq(REFRESH_TTL)))
                    .willReturn(Optional.of(MJ));
            given(adminAccountService.isAllowed(MJ)).willReturn(true);
            given(adminTokenIssuer.issue(MJ)).willReturn("new-admin-token");

            final AdminTokens tokens = adminAuthService.refresh(presented.value());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tokens.accessToken()).isEqualTo("new-admin-token");
                softly.assertThat(tokens.refreshToken().familyId()).isEqualTo(presented.familyId());
                softly.assertThat(tokens.refreshToken().tokenId()).isNotEqualTo(presented.tokenId());
            });
        }

        @Test
        void 허용목록에서_빠졌으면_회전하지_않고_family를_폐기한다() {
            given(adminRefreshTokenRepository.findEmail(presented)).willReturn(Optional.of(MJ));
            given(adminAccountService.isAllowed(MJ)).willReturn(false);

            assertCoffeeShoutException(
                    () -> adminAuthService.refresh(presented.value()), AdminAccountErrorCode.NOT_ADMIN);
            then(adminRefreshTokenRepository).should().revoke(presented.familyId());
            then(adminRefreshTokenRepository).should(never()).rotate(any(), any(), any());
            then(adminTokenIssuer).should(never()).issue(any());
        }

        @Test
        void family가_없으면_허용목록을_보지_않고_거부한다() {
            // 만료되었거나 폐기된 경우다.
            given(adminRefreshTokenRepository.findEmail(presented)).willReturn(Optional.empty());

            assertCoffeeShoutException(
                    () -> adminAuthService.refresh(presented.value()),
                    AdminAccountErrorCode.ADMIN_REFRESH_TOKEN_INVALID);
            then(adminAccountService).should(never()).isAllowed(any());
            then(adminTokenIssuer).should(never()).issue(any());
        }

        @Test
        void 회전에_실패하면_거부한다() {
            // 이미 쓴 토큰이 다시 온 경우다. 저장소가 family 를 지우고 빈 값을 준다.
            given(adminRefreshTokenRepository.findEmail(presented)).willReturn(Optional.of(MJ));
            given(adminAccountService.isAllowed(MJ)).willReturn(true);
            given(adminRefreshTokenRepository.rotate(eq(presented), any(), eq(REFRESH_TTL)))
                    .willReturn(Optional.empty());

            assertCoffeeShoutException(
                    () -> adminAuthService.refresh(presented.value()),
                    AdminAccountErrorCode.ADMIN_REFRESH_TOKEN_INVALID);
            then(adminTokenIssuer).should(never()).issue(any());
        }

        @Test
        void 허용목록_확인이_실패하면_토큰을_회전하지_않는다() {
            // 회전한 뒤에 실패하면 브라우저는 새 쿠키를 받지 못한다. 다음 재발급이 이전 토큰을 내서
            // 재사용으로 판정되고, DB 가 잠깐 끊긴 것만으로 로그인이 끊긴다.
            given(adminRefreshTokenRepository.findEmail(presented)).willReturn(Optional.of(MJ));
            given(adminAccountService.isAllowed(MJ)).willThrow(new IllegalStateException("DB 연결 실패"));

            assertThatThrownBy(() -> adminAuthService.refresh(presented.value()))
                    .isInstanceOf(IllegalStateException.class);
            then(adminRefreshTokenRepository).should(never()).rotate(any(), any(), any());
        }

        @Test
        void 쿠키가_없거나_망가졌으면_저장소를_보지_않고_거부한다() {
            assertCoffeeShoutException(
                    () -> adminAuthService.refresh(null), AdminAccountErrorCode.ADMIN_REFRESH_TOKEN_INVALID);
            assertCoffeeShoutException(
                    () -> adminAuthService.refresh("망가진값"), AdminAccountErrorCode.ADMIN_REFRESH_TOKEN_INVALID);
            then(adminRefreshTokenRepository).should(never()).findEmail(any());
        }
    }

    @Nested
    class logout {

        @Test
        void family를_폐기한다() {
            final AdminRefreshToken token = AdminRefreshToken.newFamily();

            adminAuthService.logout(token.value());

            then(adminRefreshTokenRepository).should().revoke(token.familyId());
        }

        @Test
        void 쿠키가_없어도_실패하지_않는다() {
            adminAuthService.logout(null);

            then(adminRefreshTokenRepository).should(never()).revoke(any());
        }
    }
}
