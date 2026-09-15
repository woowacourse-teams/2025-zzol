package coffeeshout.admin.auth;

import coffeeshout.admin.account.domain.AdminEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 관리자 인증 설정.
 *
 * <p>{@code emails}는 환경변수 {@code ADMIN_EMAILS}로 주입하는 부트스트랩 허용목록이다.
 * DB {@code admin_account} 테이블과 합집합으로 판정하되, 이 목록은 백오피스 UI에서 삭제할 수 없다.
 * 관리자가 실수로 자기들을 전부 지워 아무도 못 들어가는 상태를 막는 break-glass 경로다.
 *
 * <p>바인딩은 {@code List<String>}으로 받는다. 환경변수에 오타가 있어도 앱이 뜨긴 해야 하므로,
 * 형식이 어긋난 항목은 예외 대신 조용히 걸러낸다. 대신 남은 항목은 전부 정규화된 {@link AdminEmail}이다.
 */
@Validated
@ConfigurationProperties(prefix = "admin.auth")
public record AdminAuthProperties(
        List<String> emails,
        String googleClientId,

        @NotBlank @Size(min = 32, message = "관리자 JWT secret은 HS256 최소 키 길이(32자) 이상이어야 합니다.")
        String jwtSecret,

        @Positive long tokenValiditySeconds) {

    public boolean isBootstrap(AdminEmail email) {
        return email != null && bootstrapEmails().contains(email);
    }

    /** 순서를 유지해야 목록 화면에서 부트스트랩 관리자가 매번 같은 자리에 보인다. */
    public Set<AdminEmail> bootstrapEmails() {
        if (emails == null) {
            return Set.of();
        }
        final Set<AdminEmail> parsed = new LinkedHashSet<>();
        for (String each : emails) {
            AdminEmail.parse(each).ifPresent(parsed::add);
        }
        return parsed;
    }
}
