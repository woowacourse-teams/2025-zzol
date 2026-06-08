package coffeeshout.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.UserModuleIntegrationTest;
import coffeeshout.fixture.UserFixture;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.RefreshTokenRepository;
import coffeeshout.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 트랜잭션 커밋 후 동작(AFTER_COMMIT 리스너)을 검증하므로 실제 ApplicationEventPublisher가 필요하다.
 * MockEventPublisherConfig를 임포트하는 ServiceTest 대신 통합 테스트 베이스를 사용한다.
 */
class UserWithdrawalServiceTransactionTest extends UserModuleIntegrationTest {

    @Autowired
    UserWithdrawalService userWithdrawalService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    private Long userId;
    private String userCode;

    @BeforeEach
    void setUp() {
        final User user = userRepository.save(UserFixture.회원_엠제이());
        userId = user.getId();
        userCode = user.getUserCode().value();
    }

    @Nested
    class 회원_탈퇴_트랜잭션_커밋_후 {

        @Test
        void 리프레시_토큰이_모두_무효화된다() {
            final String tokenId = "test-token-id";
            refreshTokenRepository.save(userId, userCode, tokenId, 3600L);

            userWithdrawalService.withdraw(userId);

            assertThat(refreshTokenRepository.findByTokenId(tokenId)).isEmpty();
        }
    }
}
