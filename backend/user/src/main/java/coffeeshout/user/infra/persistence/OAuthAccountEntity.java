package coffeeshout.user.infra.persistence;

import coffeeshout.user.infra.crypto.EmailEncryptConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "oauth_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false, length = 255)
    private String providerUserId;

    @Convert(converter = EmailEncryptConverter.class)
    @Column(length = 512)
    private String email;

    @Column(name = "email_hash", length = 64)
    private String emailHash;

    @Column(nullable = false)
    private Instant linkedAt;

    public OAuthAccountEntity(UserEntity user, String provider, String providerUserId, String email, String emailHash) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.emailHash = emailHash;
        this.linkedAt = Instant.now();
    }
}
