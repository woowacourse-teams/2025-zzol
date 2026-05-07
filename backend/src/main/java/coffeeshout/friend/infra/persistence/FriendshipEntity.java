package coffeeshout.friend.infra.persistence;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.FriendshipStatus;
import coffeeshout.user.infra.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(
        name = "friendship",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(
                name = "uk_friendship_pair",
                columnNames = {"requester_id", "addressee_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendshipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false, updatable = false)
    private UserEntity requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false, updatable = false)
    private UserEntity addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendshipStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public FriendshipEntity(UserEntity requester, UserEntity addressee, FriendshipStatus status,
                            Instant createdAt, Instant updatedAt) {
        this.requester = requester;
        this.addressee = addressee;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
        this.updatedAt = Instant.now();
    }

    public Friendship toDomain() {
        return new Friendship(id, requester.getId(), addressee.getId(), status, createdAt, updatedAt);
    }
}
