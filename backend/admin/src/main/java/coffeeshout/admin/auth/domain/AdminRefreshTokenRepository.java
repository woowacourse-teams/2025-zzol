package coffeeshout.admin.auth.domain;

import coffeeshout.admin.account.domain.AdminEmail;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface AdminRefreshTokenRepository {

    /** 새 family 를 연다. 로그인 때 부른다. {@code loggedInAt} 은 로그인 유지 상한의 기준이다. */
    void save(AdminRefreshToken token, AdminEmail email, Duration ttl, Instant loggedInAt);

    /** 그 토큰의 family 가 가리키는 관리자. family 가 없으면 빈 값이다. tokenId 는 보지 않는다. */
    Optional<AdminEmail> findEmail(AdminRefreshToken token);

    /**
     * {@code presented} 가 그 family 의 현재 토큰이면 {@code next} 로 바꾸고 TTL 을 다시 건다.
     *
     * <p>현재 토큰이 아니면 이미 한 번 쓰인 토큰을 누군가 다시 낸 것이다. 그 family 를 지운다.
     *
     * <p>{@code loggedInBefore} 보다 먼저 로그인한 family 는 로그인 유지 상한이 지난 것이다. 지우고 거절한다.
     *
     * @return 회전에 성공했으면 그 family 의 관리자. family 가 없거나, 상한이 지났거나, 재사용이면 빈 값
     */
    Optional<AdminEmail> rotate(
            AdminRefreshToken presented, AdminRefreshToken next, Duration ttl, Instant loggedInBefore);

    void revoke(String familyId);
}
