package coffeeshout.profanity.infra.persistence;

import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profanity_word")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfanityWordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String word;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WordSource source;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private long matchCount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public static ProfanityWordEntity from(ProfanityWord domain) {
        final ProfanityWordEntity entity = new ProfanityWordEntity();
        entity.word = domain.word();
        entity.language = domain.language();
        entity.source = domain.source();
        entity.isActive = true;
        entity.matchCount = 0L;
        return entity;
    }

    public ProfanityWord toDomain() {
        return ProfanityWord.of(word, language, source);
    }

    public boolean reactivate() {
        if (this.isActive) {
            return false;
        }
        this.isActive = true;
        return true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void incrementMatchCount() {
        this.matchCount++;
    }
}
