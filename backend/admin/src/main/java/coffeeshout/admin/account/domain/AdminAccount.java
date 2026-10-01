package coffeeshout.admin.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UI로 추가한 관리자. 환경변수 부트스트랩 관리자는 이 테이블에 없다.
 *
 * <p>ADR-0019가 정한 대로 도메인 객체가 JPA 어노테이션을 직접 갖는다.
 * 컬럼은 문자열이지만 밖으로는 {@link AdminEmail}만 내보내 정규화되지 않은 값이 새지 않게 한다.
 */
@Entity
@Table(name = "admin_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** 누가 이 관리자를 추가했는지. 시스템이 넣은 경우 null. */
    @Column(name = "created_by_email", length = 255)
    private String createdByEmail;

    @Getter
    @Column(nullable = false)
    private Instant createdAt;

    public static AdminAccount create(AdminEmail email, AdminEmail createdBy, Instant now) {
        final AdminAccount account = new AdminAccount();
        account.email = email.value();
        account.createdByEmail = createdBy == null ? null : createdBy.value();
        account.createdAt = now;
        return account;
    }

    public AdminEmail getEmail() {
        return new AdminEmail(email);
    }

    /** 시스템이 추가한 경우 null. */
    public AdminEmail getCreatedByEmail() {
        return createdByEmail == null ? null : new AdminEmail(createdByEmail);
    }
}
