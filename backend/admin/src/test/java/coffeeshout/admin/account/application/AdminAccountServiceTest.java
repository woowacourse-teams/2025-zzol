package coffeeshout.admin.account.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.admin.account.domain.AdminAccount;
import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import coffeeshout.admin.account.domain.AdminAccountRepository;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.AdminAuthProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AdminAccountService")
@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String SECRET = "x".repeat(32);

    private static final AdminEmail BOOTSTRAP = AdminEmail.of("root@zzol.site");
    private static final AdminEmail MJ = AdminEmail.of("mj@zzol.site");
    private static final AdminEmail STRANGER = AdminEmail.of("stranger@evil.site");

    @Mock
    private AdminAccountRepository adminAccountRepository;

    private AdminAccountService service(List<String> bootstrapEmails) {
        return new AdminAccountService(
                adminAccountRepository, new AdminAuthProperties(bootstrapEmails, "client-id", SECRET, 3600), CLOCK);
    }

    private AdminAccountService serviceWithBootstrap() {
        return service(List.of(BOOTSTRAP.value()));
    }

    private AdminAccount account(AdminEmail email, AdminEmail createdBy) {
        return AdminAccount.create(email, createdBy, NOW);
    }

    @Nested
    class isAllowed {

        @Test
        void 부트스트랩_이메일은_DB를_보지_않고_허용한다() {
            assertThat(serviceWithBootstrap().isAllowed(BOOTSTRAP)).isTrue();
            // DB가 죽어도 break-glass 계정은 들어올 수 있어야 하므로 조회 자체를 하지 않는다.
            then(adminAccountRepository).should(never()).existsByEmail(any());
        }

        @Test
        void DB에_있으면_허용한다() {
            given(adminAccountRepository.existsByEmail("mj@zzol.site")).willReturn(true);

            assertThat(serviceWithBootstrap().isAllowed(MJ)).isTrue();
        }

        @Test
        void 양쪽_어디에도_없으면_거부한다() {
            given(adminAccountRepository.existsByEmail("stranger@evil.site")).willReturn(false);

            assertThat(serviceWithBootstrap().isAllowed(STRANGER)).isFalse();
        }

        @Test
        void 환경변수의_대소문자가_달라도_판정한다() {
            assertThat(service(List.of("  ROOT@Zzol.Site ")).isAllowed(BOOTSTRAP))
                    .isTrue();
        }

        @Test
        void 환경변수의_형식_오류_항목은_조용히_무시한다() {
            // 오타 하나로 앱이 아예 안 뜨면 배포가 막힌다. 나머지는 살린다.
            final AdminAccountService service = service(List.of("오타", "", BOOTSTRAP.value()));

            assertThat(service.isAllowed(BOOTSTRAP)).isTrue();
        }

        @Test
        void null은_DB를_보지_않고_거부한다() {
            assertThat(serviceWithBootstrap().isAllowed(null)).isFalse();
            then(adminAccountRepository).should(never()).existsByEmail(any());
        }
    }

    @Nested
    class list {

        @Test
        void 부트스트랩을_먼저_두고_DB를_뒤에_붙인다() {
            given(adminAccountRepository.findAllByOrderByCreatedAtAsc()).willReturn(List.of(account(MJ, BOOTSTRAP)));

            final List<AdminAccountEntry> entries = serviceWithBootstrap().list();

            assertThat(entries).extracting(AdminAccountEntry::email).containsExactly(BOOTSTRAP, MJ);
            assertThat(entries)
                    .extracting(AdminAccountEntry::source)
                    .containsExactly(AdminAccountSource.BOOTSTRAP, AdminAccountSource.DATABASE);
        }

        @Test
        void 부트스트랩_항목은_삭제할_수_없다() {
            given(adminAccountRepository.findAllByOrderByCreatedAtAsc()).willReturn(List.of());

            assertThat(serviceWithBootstrap().list())
                    .singleElement()
                    .extracting(AdminAccountEntry::removable)
                    .isEqualTo(false);
        }

        @Test
        void DB_항목은_삭제할_수_있다() {
            given(adminAccountRepository.findAllByOrderByCreatedAtAsc()).willReturn(List.of(account(MJ, BOOTSTRAP)));

            assertThat(service(List.of()).list())
                    .singleElement()
                    .extracting(AdminAccountEntry::removable)
                    .isEqualTo(true);
        }

        @Test
        void 양쪽에_같은_이메일이_있으면_부트스트랩_줄만_남긴다() {
            // 두 줄로 보이면 삭제 가능한 줄을 지우고도 로그인이 되는 것을 버그로 오해한다.
            given(adminAccountRepository.findAllByOrderByCreatedAtAsc()).willReturn(List.of(account(BOOTSTRAP, null)));

            assertThat(serviceWithBootstrap().list())
                    .singleElement()
                    .extracting(AdminAccountEntry::source)
                    .isEqualTo(AdminAccountSource.BOOTSTRAP);
        }
    }

    @Nested
    class add {

        @Test
        void 추가한_관리자를_실행자와_함께_저장한다() {
            given(adminAccountRepository.existsByEmail("mj@zzol.site")).willReturn(false);
            given(adminAccountRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            final AdminAccountEntry entry = serviceWithBootstrap().add(MJ, BOOTSTRAP);

            assertThat(entry.email()).isEqualTo(MJ);
            assertThat(entry.createdByEmail()).isEqualTo(BOOTSTRAP);
            assertThat(entry.createdAt()).isEqualTo(NOW);
        }

        @Test
        void 이미_DB에_있으면_거부한다() {
            given(adminAccountRepository.existsByEmail("mj@zzol.site")).willReturn(true);

            assertCoffeeShoutException(
                    () -> serviceWithBootstrap().add(MJ, BOOTSTRAP),
                    AdminAccountErrorCode.ADMIN_ACCOUNT_ALREADY_EXISTS);
        }

        @Test
        void 부트스트랩에_있는_이메일도_거부한다() {
            // DB에 넣어봐야 list()가 감추므로, 넣히는 것 자체를 막아 혼란을 없앤다.
            assertCoffeeShoutException(
                    () -> serviceWithBootstrap().add(BOOTSTRAP, BOOTSTRAP),
                    AdminAccountErrorCode.ADMIN_ACCOUNT_ALREADY_EXISTS);
            then(adminAccountRepository).should(never()).save(any());
        }
    }

    @Nested
    class remove {

        @Test
        void DB_관리자를_삭제한다() {
            final AdminAccount target = account(MJ, BOOTSTRAP);
            given(adminAccountRepository.findById(1L)).willReturn(Optional.of(target));

            serviceWithBootstrap().remove(1L, BOOTSTRAP);

            then(adminAccountRepository).should().delete(target);
        }

        @Test
        void 자기_자신은_삭제할_수_없다() {
            given(adminAccountRepository.findById(1L)).willReturn(Optional.of(account(MJ, BOOTSTRAP)));

            assertCoffeeShoutException(
                    () -> serviceWithBootstrap().remove(1L, AdminEmail.of("  MJ@Zzol.Site ")),
                    AdminAccountErrorCode.CANNOT_REMOVE_SELF);
            then(adminAccountRepository).should(never()).delete(any());
        }

        @Test
        void 없는_id면_거부한다() {
            given(adminAccountRepository.findById(404L)).willReturn(Optional.empty());

            assertCoffeeShoutException(
                    () -> serviceWithBootstrap().remove(404L, BOOTSTRAP),
                    AdminAccountErrorCode.ADMIN_ACCOUNT_NOT_FOUND);
        }
    }
}
