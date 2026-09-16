package coffeeshout.admin.account.ui.response;

import coffeeshout.admin.account.application.AdminAccountEntry;
import coffeeshout.admin.account.application.AdminAccountSource;
import java.time.Instant;

/**
 * @param removable 부트스트랩(환경변수) 관리자는 false. 화면이 삭제 버튼을 감추는 근거다.
 */
public record AdminAccountResponse(
        Long id, String email, AdminAccountSource source, boolean removable, String createdByEmail, Instant createdAt) {

    public static AdminAccountResponse from(AdminAccountEntry entry) {
        return new AdminAccountResponse(
                entry.id(),
                entry.email().value(),
                entry.source(),
                entry.removable(),
                entry.createdByEmail() == null ? null : entry.createdByEmail().value(),
                entry.createdAt());
    }
}
