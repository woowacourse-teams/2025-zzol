package coffeeshout.room.infra.nickname.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "custom_profanity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomProfanityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String word;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source;

    @Column(nullable = false)
    private Instant createdAt;

    public CustomProfanityEntity(String word, Source source) {
        this.word = word;
        this.source = source;
        this.createdAt = Instant.now();
    }

    public enum Source {
        AI_AUDIT, OPERATOR_MANUAL
    }
}
