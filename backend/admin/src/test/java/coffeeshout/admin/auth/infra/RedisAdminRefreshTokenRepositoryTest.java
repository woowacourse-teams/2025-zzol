package coffeeshout.admin.auth.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.AdminModuleServiceTest;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.domain.AdminRefreshToken;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisAdminRefreshTokenRepositoryTest extends AdminModuleServiceTest {

    private static final String KEY_PREFIX = "admin:refresh:family:";
    private static final AdminEmail MJ = AdminEmail.of("mj@zzol.site");
    private static final Duration TTL = Duration.ofSeconds(60);

    @Autowired
    RedisAdminRefreshTokenRepository repository;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    void 현재_토큰이면_회전하고_관리자를_돌려준다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL);

        final AdminRefreshToken next = token.rotate();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(repository.rotate(token, next, TTL)).contains(MJ);
            softly.assertThat(stringRedisTemplate.opsForHash().get(KEY_PREFIX + token.familyId(), "tokenId"))
                    .isEqualTo(next.tokenId());
        });
    }

    @Test
    void 회전할_때마다_TTL을_다시_건다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, Duration.ofSeconds(10));

        repository.rotate(token, token.rotate(), Duration.ofSeconds(600));

        assertThat(stringRedisTemplate.getExpire(KEY_PREFIX + token.familyId())).isGreaterThan(10L);
    }

    @Test
    void 이미_쓴_토큰을_다시_내면_family를_지운다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL);
        final AdminRefreshToken next = token.rotate();
        repository.rotate(token, next, TTL);

        final Optional<AdminEmail> reused = repository.rotate(token, token.rotate(), TTL);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(reused).isEmpty();
            softly.assertThat(stringRedisTemplate.hasKey(KEY_PREFIX + token.familyId()))
                    .isFalse();
            // 정상 사용자가 가진 최신 토큰도 함께 무효가 된다. 누가 탈취자인지 서버는 모른다.
            softly.assertThat(repository.rotate(next, next.rotate(), TTL)).isEmpty();
        });
    }

    @Test
    void family의_관리자를_읽는다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(repository.findEmail(token)).contains(MJ);
            // 재발급 전에 허용목록을 보려고 읽는 것이라 tokenId 는 따지지 않는다. 재사용 판정은 회전이 한다.
            softly.assertThat(repository.findEmail(token.rotate())).contains(MJ);
            softly.assertThat(repository.findEmail(AdminRefreshToken.newFamily()))
                    .isEmpty();
        });
    }

    @Test
    void 없는_family면_빈_값이다() {
        final AdminRefreshToken unknown = AdminRefreshToken.newFamily();

        assertThat(repository.rotate(unknown, unknown.rotate(), TTL)).isEmpty();
    }

    @Test
    void 같은_토큰으로_동시에_회전하면_하나만_성공한다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL);

        final List<CompletableFuture<Optional<AdminEmail>>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> repository.rotate(token, token.rotate(), TTL)));
        }
        final long succeeded = futures.stream()
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .count();

        assertThat(succeeded).isEqualTo(1);
    }

    @Test
    void 폐기하면_회전할_수_없다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        repository.save(token, MJ, TTL);

        repository.revoke(token.familyId());

        assertThat(repository.rotate(token, token.rotate(), TTL)).isEmpty();
    }

    @Test
    void TTL이_0이면_저장하지_않고_예외를_던진다() {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();

        // @Repository 예외 변환이 IllegalArgumentException 을 감쌀 수 있어 원인으로 본다.
        assertThatThrownBy(() -> repository.save(token, MJ, Duration.ZERO))
                .satisfiesAnyOf(e -> assertThat(e).isInstanceOf(IllegalArgumentException.class), e -> assertThat(e)
                        .hasRootCauseInstanceOf(IllegalArgumentException.class));
        assertThat(stringRedisTemplate.hasKey(KEY_PREFIX + token.familyId())).isFalse();
    }
}
