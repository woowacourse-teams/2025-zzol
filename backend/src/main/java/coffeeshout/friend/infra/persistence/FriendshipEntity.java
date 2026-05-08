package coffeeshout.friend.infra.persistence;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.FriendshipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "friendship",
        uniqueConstraints = @UniqueConstraint(
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

    @Column(name = "requester_id", nullable = false, updatable = false)
    private Long requesterId;

    @Column(name = "addressee_id", nullable = false, updatable = false)
    private Long addresseeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendshipStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public FriendshipEntity(Long requesterId, Long addresseeId, FriendshipStatus status,
                            Instant createdAt, Instant updatedAt) {
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
        this.updatedAt = Instant.now();
    }

    public Friendship toDomain() {
        return new Friendship(id, requesterId, addresseeId, status, createdAt, updatedAt);
    }
}
