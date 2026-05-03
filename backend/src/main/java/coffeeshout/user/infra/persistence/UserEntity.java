package coffeeshout.user.infra.persistence;

import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.UserNickname;
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

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_code", nullable = false, unique = true, updatable = false, length = 5)
    private String userCode;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(name = "terms_agreed_at")
    private Instant termsAgreedAt;

    public UserEntity(String userCode, String nickname) {
        final Instant now = Instant.now();
        this.userCode = userCode;
        this.nickname = nickname;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = Instant.now();
    }

    public void agreeTerms() {
        this.termsAgreedAt = Instant.now();
    }

    public User toDomain(OAuthAccountEntity oAuthAccountEntity) {
        final OAuthAccount oAuthAccount = new OAuthAccount(
                OAuthProvider.from(oAuthAccountEntity.getProvider()),
                oAuthAccountEntity.getProviderUserId(),
                oAuthAccountEntity.getEmail()
        );
        return new User(id, new UserCode(userCode), new UserNickname(nickname), oAuthAccount);
    }
}
