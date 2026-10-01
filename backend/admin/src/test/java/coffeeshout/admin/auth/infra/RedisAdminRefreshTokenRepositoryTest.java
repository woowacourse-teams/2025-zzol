package coffeeshout.admin.auth.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.domain.AdminRefreshToken;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisAdminRefreshTokenRepositoryTest extends AdminModuleServiceTest {

    private static final String KEY_PREFIX = "admin:refresh:family:";
    private static final AdminEmail MJ = AdminEmail.of("mj@zzol.site");
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final Instant LOGGED_IN_AT = Instant.parse("2026-09-01T00:00:00Z");
    // 로그인 시각보다 이른 기준. 로그인 유지 상한이 아직 지나지 않은 상태다.
    private static final Instant WITHIN_LIMIT = LOGGED_IN_AT.minusSeconds(1);

    @Autowired
    RedisAdminRefreshTokenRepository repository;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    void 현재_토큰이면_회전하고_관리자를_돌려준다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL, LOGGED_IN_AT);

        final AdminRefreshToken next = token.rotate();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(repository.rotate(token, next, TTL, WITHIN_LIMIT)).contains(MJ);
            softly.assertThat(stringRedisTemplate.opsForHash().get(KEY_PREFIX + token.familyId(), "tokenId"))
                    .isEqualTo(next.tokenId());
        });
    }

    @Test
    void 회전할_때마다_TTL을_다시_건다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, Duration.ofSeconds(10), LOGGED_IN_AT);

        repository.rotate(token, token.rotate(), Duration.ofSeconds(600), WITHIN_LIMIT);

        assertThat(stringRedisTemplate.getExpire(KEY_PREFIX + token.familyId())).isGreaterThan(10L);
    }

    @Test
    void 이미_쓴_토큰을_다시_내면_family를_지운다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL, LOGGED_IN_AT);
        final AdminRefreshToken next = token.rotate();
        repository.rotate(token, next, TTL, WITHIN_LIMIT);

        final Optional<AdminEmail> reused = repository.rotate(token, token.rotate(), TTL, WITHIN_LIMIT);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(reused).isEmpty();
            softly.assertThat(stringRedisTemplate.hasKey(KEY_PREFIX + token.familyId()))
                    .isFalse();
            // 정상 사용자가 가진 최신 토큰도 함께 무효가 된다. 누가 탈취자인지 서버는 모른다.
            softly.assertThat(repository.rotate(next, next.rotate(), TTL, WITHIN_LIMIT))
                    .isEmpty();
        });
    }

    @Test
    void family의_관리자를_읽는다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL, LOGGED_IN_AT);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(repository.findEmail(token)).contains(MJ);
            // 재발급 전에 허용목록을 보려고 읽는 것이라 tokenId 는 따지지 않는다. 재사용 판정은 회전이 한다.
            softly.assertThat(repository.findEmail(token.rotate())).contains(MJ);
            softly.assertThat(repository.findEmail(AdminRefreshToken.newFamily()))
                    .isEmpty();
        });
    }

    @Test
    void 로그인_유지_상한이_지났으면_회전하지_않고_family를_지운다() {
        // 7일 안에 한 번씩 재발급하면 무기한 쓸 수 있으므로 로그인 시점부터 상한을 둔다.
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL, LOGGED_IN_AT);

        final Optional<AdminEmail> rotated = repository.rotate(token, token.rotate(), TTL, LOGGED_IN_AT.plusSeconds(1));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(rotated).isEmpty();
            softly.assertThat(stringRedisTemplate.hasKey(KEY_PREFIX + token.familyId()))
                    .isFalse();
        });
    }

    @Test
    void 로그인_시각이_기준과_같으면_아직_회전한다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL, LOGGED_IN_AT);

        assertThat(repository.rotate(token, token.rotate(), TTL, LOGGED_IN_AT)).contains(MJ);
    }

    @Test
    void 없는_family면_빈_값이다() {
        final AdminRefreshToken unknown = AdminRefreshToken.newFamily();

        assertThat(repository.rotate(unknown, unknown.rotate(), TTL, WITHIN_LIMIT))
                .isEmpty();
    }

    @Test
    void 같은_토큰으로_동시에_회전하면_하나만_성공한다() throws Exception {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL, LOGGED_IN_AT);

        // 모든 스레드를 출발선에 세웠다가 한 번에 보낸다. 풀에 차례로 넣기만 하면 앞 요청이 회전을
        // 끝낸 뒤 다음 요청이 시작될 수 있고, 그러면 조회와 교체를 따로 보내는 구현도 통과한다.
        final int requests = 8;
        final CountDownLatch ready = new CountDownLatch(requests);
        final CountDownLatch start = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(requests);
        try {
            final List<Future<Optional<AdminEmail>>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return repository.rotate(token, token.rotate(), TTL, WITHIN_LIMIT);
                }));
            }
            ready.await();
            start.countDown();

            long succeeded = 0;
            for (Future<Optional<AdminEmail>> future : futures) {
                if (future.get(10, TimeUnit.SECONDS).isPresent()) {
                    succeeded++;
                }
            }
            assertThat(succeeded).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void 폐기하면_회전할_수_없다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL, LOGGED_IN_AT);

        repository.revoke(token.familyId());

        assertThat(repository.rotate(token, token.rotate(), TTL, WITHIN_LIMIT)).isEmpty();
    }

    @Test
    void TTL이_0이면_저장하지_않고_예외를_던진다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();

        // @Repository 예외 변환이 IllegalArgumentException 을 감쌀 수 있어 원인으로 본다.
        assertThatThrownBy(() -> repository.save(token, MJ, Duration.ZERO, LOGGED_IN_AT))
                .satisfiesAnyOf(e -> assertThat(e).isInstanceOf(IllegalArgumentException.class), e -> assertThat(e)
                        .hasRootCauseInstanceOf(IllegalArgumentException.class));
        assertThat(stringRedisTemplate.hasKey(KEY_PREFIX + token.familyId())).isFalse();
    }
}
