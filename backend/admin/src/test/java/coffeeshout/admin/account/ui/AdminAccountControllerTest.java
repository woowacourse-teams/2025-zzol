package coffeeshout.admin.account.ui;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import coffeeshout.admin.account.application.AdminAccountEntry;
import coffeeshout.admin.account.application.AdminAccountService;
import coffeeshout.admin.account.application.AdminAccountSource;
import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.account.ui.request.AddAdminAccountRequest;
import coffeeshout.admin.account.ui.response.AdminAccountResponse;
import coffeeshout.admin.auth.domain.AdminPrincipal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AdminAccountController")
@ExtendWith(MockitoExtension.class)
class AdminAccountControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");
    private static final AdminEmail ROOT = AdminEmail.of("root@zzol.site");
    private static final AdminEmail MJ = AdminEmail.of("mj@zzol.site");
    private static final AdminPrincipal ACTOR = new AdminPrincipal(ROOT);

    @Mock
    private AdminAccountService adminAccountService;

    @InjectMocks
    private AdminAccountController adminAccountController;

    @Nested
    class list {

        @Test
        void 부트스트랩과_DB_항목을_함께_돌려준다() {
            given(adminAccountService.list())
                    .willReturn(
                            List.of(AdminAccountEntry.bootstrap(ROOT), AdminAccountEntry.database(1L, MJ, ROOT, NOW)));

            final List<AdminAccountResponse> responses = adminAccountController.list();

            assertThat(responses)
                    .extracting(AdminAccountResponse::email)
                    .containsExactly("root@zzol.site", "mj@zzol.site");
            assertThat(responses)
                    .extracting(AdminAccountResponse::source)
                    .containsExactly(AdminAccountSource.BOOTSTRAP, AdminAccountSource.DATABASE);
        }

        @Test
        void 부트스트랩_항목은_삭제_불가로_표시한다() {
            given(adminAccountService.list()).willReturn(List.of(AdminAccountEntry.bootstrap(ROOT)));

            assertThat(adminAccountController.list()).singleElement().satisfies(response -> {
                assertThat(response.removable()).isFalse();
                assertThat(response.id()).isNull();
            });
        }

        @Test
        void 항목이_없으면_빈_목록을_돌려준다() {
            given(adminAccountService.list()).willReturn(List.of());

            assertThat(adminAccountController.list()).isEmpty();
        }
    }

    @Nested
    class add {

        @Test
        void 실행자를_추가자로_넘긴다() {
            given(adminAccountService.add(MJ, ROOT)).willReturn(AdminAccountEntry.database(1L, MJ, ROOT, NOW));

            final AdminAccountResponse response =
                    adminAccountController.add(new AddAdminAccountRequest("mj@zzol.site"), ACTOR);

            assertThat(response.email()).isEqualTo("mj@zzol.site");
            assertThat(response.createdByEmail()).isEqualTo("root@zzol.site");
            then(adminAccountService).should().add(MJ, ROOT);
        }

        @Test
        void 입력값을_정규화해_서비스에_넘긴다() {
            given(adminAccountService.add(MJ, ROOT)).willReturn(AdminAccountEntry.database(1L, MJ, ROOT, NOW));

            adminAccountController.add(new AddAdminAccountRequest("  MJ@Zzol.Site "), ACTOR);

            then(adminAccountService).should().add(MJ, ROOT);
        }

        @ParameterizedTest
        @ValueSource(strings = {"nobody", "@zzol.site", "mj@", "   "})
        void 형식이_틀리면_틀렸다고_알려준다(String email) {
            // 관리자가 직접 입력한 값이다. 로그인 판정과 달리 숨길 이유가 없다.
            assertCoffeeShoutException(
                    () -> adminAccountController.add(new AddAdminAccountRequest(email), ACTOR),
                    AdminAccountErrorCode.INVALID_ADMIN_EMAIL);
            then(adminAccountService).shouldHaveNoInteractions();
        }
    }

    @Nested
    class remove {

        @Test
        void 실행자와_함께_삭제를_위임한다() {
            adminAccountController.remove(1L, ACTOR);

            then(adminAccountService).should().remove(1L, ROOT);
        }
    }
}
