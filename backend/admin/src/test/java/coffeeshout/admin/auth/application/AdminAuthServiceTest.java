package coffeeshout.admin.auth.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.admin.account.application.AdminAccountService;
import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.domain.AdminTokenIssuer;
import coffeeshout.admin.auth.domain.SocialIdTokenVerifier;
import coffeeshout.global.exception.custom.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AdminAuthService")
@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    private static final String ID_TOKEN = "google-id-token";
    private static final AdminEmail MJ = AdminEmail.of("mj@zzol.site");
    private static final AdminEmail STRANGER = AdminEmail.of("stranger@evil.site");

    @Mock
    private SocialIdTokenVerifier socialIdTokenVerifier;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminTokenIssuer adminTokenIssuer;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Nested
    class login {

        @Test
        void 허용목록에_있으면_관리자_토큰을_발급한다() {
            given(socialIdTokenVerifier.verifyAndExtractEmail(ID_TOKEN)).willReturn("mj@zzol.site");
            given(adminAccountService.isAllowed(MJ)).willReturn(true);
            given(adminTokenIssuer.issue(MJ)).willReturn("admin-token");

            assertThat(adminAuthService.login(ID_TOKEN)).isEqualTo("admin-token");
        }

        @Test
        void 구글이_준_이메일을_정규화해_판정하고_발급한다() {
            given(socialIdTokenVerifier.verifyAndExtractEmail(ID_TOKEN)).willReturn("  MJ@Zzol.Site ");
            given(adminAccountService.isAllowed(MJ)).willReturn(true);
            given(adminTokenIssuer.issue(MJ)).willReturn("admin-token");

            assertThat(adminAuthService.login(ID_TOKEN)).isEqualTo("admin-token");
        }

        @Test
        void 허용목록에_없으면_토큰을_발급하지_않는다() {
            given(socialIdTokenVerifier.verifyAndExtractEmail(ID_TOKEN)).willReturn("stranger@evil.site");
            given(adminAccountService.isAllowed(STRANGER)).willReturn(false);

            assertCoffeeShoutException(() -> adminAuthService.login(ID_TOKEN), AdminAccountErrorCode.NOT_ADMIN);
            then(adminTokenIssuer).should(never()).issue(any());
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
}
