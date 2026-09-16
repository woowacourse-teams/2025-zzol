package coffeeshout.admin.account.application;

import coffeeshout.admin.account.domain.AdminAccount;
import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import coffeeshout.admin.account.domain.AdminAccountRepository;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.AdminAuthProperties;
import coffeeshout.global.exception.custom.BusinessException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 허용목록.
 *
 * <p>판정은 <b>환경변수 부트스트랩 ∪ DB</b>다. 두 곳으로 나눈 이유는 서로 다른 실패를 막기 위해서다.
 * DB만 쓰면 관리자를 전부 지웠을 때 아무도 못 들어간다. 환경변수만 쓰면 계정 추가마다 재배포해야 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAccountService {

    private final AdminAccountRepository adminAccountRepository;
    private final AdminAuthProperties adminAuthProperties;
    private final Clock clock;

    public boolean isAllowed(AdminEmail email) {
        if (email == null) {
            return false;
        }
        // 부트스트랩을 먼저 본다. DB가 죽어도 break-glass 계정은 들어올 수 있어야 한다.
        if (adminAuthProperties.isBootstrap(email)) {
            return true;
        }
        return adminAccountRepository.existsByEmail(email.value());
    }

    public List<AdminAccountEntry> list() {
        final List<AdminAccountEntry> entries = new ArrayList<>();
        for (AdminEmail email : adminAuthProperties.bootstrapEmails()) {
            entries.add(AdminAccountEntry.bootstrap(email));
        }
        for (AdminAccount account : adminAccountRepository.findAllByOrderByCreatedAtAsc()) {
            // 같은 이메일이 양쪽에 있으면 부트스트랩 줄만 남긴다. 한 사람이 두 줄로 보이면
            // 삭제 가능한 줄을 지우고도 여전히 로그인되는 상황을 관리자가 버그로 오해한다.
            if (adminAuthProperties.isBootstrap(account.getEmail())) {
                continue;
            }
            entries.add(AdminAccountEntry.database(
                    account.getId(), account.getEmail(),
                    account.getCreatedByEmail(), account.getCreatedAt()));
        }
        return List.copyOf(entries);
    }

    @Transactional
    public AdminAccountEntry add(AdminEmail email, AdminEmail actor) {
        if (isAllowed(email)) {
            throw new BusinessException(
                    AdminAccountErrorCode.ADMIN_ACCOUNT_ALREADY_EXISTS, "이미 등록된 관리자입니다: " + email.value());
        }

        final AdminAccount saved = adminAccountRepository.save(AdminAccount.create(email, actor, clock.instant()));
        return AdminAccountEntry.database(
                saved.getId(), saved.getEmail(), saved.getCreatedByEmail(), saved.getCreatedAt());
    }

    @Transactional
    public void remove(Long id, AdminEmail actor) {
        final AdminAccount account = adminAccountRepository
                .findById(id)
                .orElseThrow(() ->
                        new BusinessException(AdminAccountErrorCode.ADMIN_ACCOUNT_NOT_FOUND, "존재하지 않는 관리자입니다: " + id));

        // 자기 자신을 지우면 그 순간 로그아웃되고, 남은 관리자가 없으면 복구 경로가 부트스트랩뿐이다.
        if (account.getEmail().equals(actor)) {
            throw new BusinessException(AdminAccountErrorCode.CANNOT_REMOVE_SELF, "자기 자신은 삭제할 수 없습니다.");
        }
        adminAccountRepository.delete(account);
    }
}
