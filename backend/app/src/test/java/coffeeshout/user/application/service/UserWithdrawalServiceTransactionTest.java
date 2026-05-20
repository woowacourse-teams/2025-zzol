package coffeeshout.user.application.service;

// TODO(phase2): UserWithdrawalService가 :admin 모듈에 존재하여 :user 단독 모듈로 이동 불가.
//  :admin 모듈 테스트로 이동 예정.
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.TestContainerSupport;
import coffeeshout.fixture.UserFixture;
import coffeeshout.support.CommonTestSchedulerConfig;
import coffeeshout.support.app.config.ServiceTestConfig;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.RefreshTokenRepository;
import coffeeshout.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import({CommonTestSchedulerConfig.class, ServiceTestConfig.class})
@ActiveProfiles("test")
class UserWithdrawalServiceTransactionTest extends TestContainerSupport {

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
        cleanDatabase();
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
