package coffeeshout.friend.domain;

import coffeeshout.friend.application.service.RelationStatus;
import coffeeshout.global.exception.custom.BusinessException;
import java.time.Instant;

public class Friendship {

    private final Long id;
    private final Long requesterId;
    private final Long addresseeId;
    private FriendshipStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Friendship(Long id, Long requesterId, Long addresseeId, FriendshipStatus status,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Friendship request(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new BusinessException(FriendErrorCode.CANNOT_FRIEND_SELF, "자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }
        final Instant now = Instant.now();
        return new Friendship(null, requesterId, addresseeId, FriendshipStatus.PENDING, now, now);
    }

    public FriendshipStatus acceptBy(Long userId) {
        validateAddresseeIs(userId);
        if (!isPending()) {
            throw new BusinessException(FriendErrorCode.FRIEND_REQUEST_INVALID_STATE, "처리할 수 없는 상태의 친구 요청입니다.");
        }
        this.status = FriendshipStatus.ACCEPTED;
        this.updatedAt = Instant.now();
        return this.status;
    }

    public void validateRejectableBy(Long userId) {
        validateAddresseeIs(userId);
        if (!isPending()) {
            throw new BusinessException(FriendErrorCode.FRIEND_REQUEST_INVALID_STATE, "처리할 수 없는 상태의 친구 요청입니다.");
        }
    }

    public boolean involves(Long userId) {
        return requesterId.equals(userId) || addresseeId.equals(userId);
    }

    public Long counterpartOf(Long userId) {
        if (requesterId.equals(userId)) {
            return addresseeId;
        }
        if (addresseeId.equals(userId)) {
            return requesterId;
        }
        throw new BusinessException(FriendErrorCode.FRIEND_REQUEST_FORBIDDEN, "해당 친구 관계에 포함되지 않은 사용자입니다.");
    }

    public RelationStatus statusFrom(Long myId) {
        if (isAccepted()) {
            return RelationStatus.FRIEND;
        }
        return requesterId.equals(myId) ? RelationStatus.PENDING_OUTGOING : RelationStatus.PENDING_INCOMING;
    }

    public boolean isPending() {
        return status == FriendshipStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == FriendshipStatus.ACCEPTED;
    }

    private void validateAddresseeIs(Long userId) {
        if (!addresseeId.equals(userId)) {
            throw new BusinessException(FriendErrorCode.FRIEND_REQUEST_FORBIDDEN, "해당 친구 요청을 처리할 권한이 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public Long getAddresseeId() {
        return addresseeId;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
