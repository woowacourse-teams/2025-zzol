package coffeeshout.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.UserFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.ReportRepository;
import coffeeshout.report.infra.persistence.Reporter;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.RefreshTokenRepository;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserWithdrawalServiceTest extends ServiceTest {

    @Autowired
    UserWithdrawalService userWithdrawalService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserJpaRepository userJpaRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    EntityManager entityManager;

    private Long userId;
    private String userCode;

    @BeforeEach
    void setUp() {
        final User user = userRepository.save(UserFixture.회원_엠제이());
        userId = user.getId();
        userCode = user.getUserCode().value();
    }

    @Nested
    class 회원_탈퇴 {

        @Test
        void 탈퇴_후_회원_정보가_삭제된다() {
            userWithdrawalService.withdraw(userId);

            // deleteById 후 REMOVED 상태인 엔티티를 em.find()가 null 반환 — flush 없이 L1 캐시로 검증
            assertThat(userJpaRepository.findById(userId)).isEmpty();
        }

        @Test
        void 탈퇴_후_리프레시_토큰이_모두_무효화된다() {
            final String tokenId = "test-token-id";
            refreshTokenRepository.save(userId, userCode, tokenId, 3600L);

            userWithdrawalService.withdraw(userId);

            assertThat(refreshTokenRepository.findByTokenId(tokenId)).isEmpty();
        }

        @Test
        void 탈퇴_후_신고의_user_code가_null로_익명화된다() {
            final Report report = Report.createBugReport(
                    MiniGameType.CARD_GAME, "ABC12", "버그가 있어요.",
                    Instant.now(), new Reporter(userId, userCode)
            );
            final Long reportId = reportRepository.save(report).getId();

            userWithdrawalService.withdraw(userId);

            // JPQL bulk update는 즉시 실행되지만 L1 캐시를 우회하므로 clear() 후 재조회
            // user_id null 처리는 DB CASCADE에 위임 (트랜잭션 커밋 시 적용)
            entityManager.clear();

            final Reporter author = reportRepository.findById(reportId).orElseThrow().getAuthor();
            assertThat(author.getUserCode()).isNull();
        }
    }
}
