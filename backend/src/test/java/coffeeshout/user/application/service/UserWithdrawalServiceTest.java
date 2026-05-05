package coffeeshout.user.application.service;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.UserFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.ReportRepository;
import coffeeshout.report.infra.persistence.Reporter;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.infra.persistence.OAuthAccountJpaRepository;
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import java.time.Instant;
import org.assertj.core.api.SoftAssertions;
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
    OAuthAccountJpaRepository oAuthAccountJpaRepository;

    @Autowired
    ReportRepository reportRepository;

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
        void 탈퇴_후_회원_엔티티는_soft_delete되고_닉네임이_익명화된다() {
            final Instant before = Instant.now();
            userWithdrawalService.withdraw(userId);

            final UserEntity entity = userJpaRepository.findByIdIgnoringDeletedAt(userId).orElseThrow();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(entity.isDeleted()).isTrue();
                softly.assertThat(entity.getNickname()).isEqualTo("탈퇴한 사용자");
                softly.assertThat(entity.getDeletedAt()).isNotNull().isAfterOrEqualTo(before);
            });
        }

        @Test
        void 탈퇴_후_도메인_조회에서_찾을_수_없다() {
            userWithdrawalService.withdraw(userId);

            assertThat(userRepository.findById(userId)).isEmpty();
        }

        @Test
        void 탈퇴_후_OAuth_계정이_hard_delete된다() {
            userWithdrawalService.withdraw(userId);

            assertThat(oAuthAccountJpaRepository.countByUserId(userId)).isZero();
        }

        @Test
        void 존재하지_않는_회원_탈퇴_시_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> userWithdrawalService.withdraw(-1L),
                    UserErrorCode.USER_NOT_FOUND
            );
        }

        @Test
        void 탈퇴_후_신고의_user_code가_null로_익명화된다() {
            final Report report = Report.createBugReport(
                    MiniGameType.CARD_GAME, "ABC12", "버그가 있어요.",
                    Instant.now(), new Reporter(userId, userCode)
            );
            final Long reportId = reportRepository.save(report).getId();

            userWithdrawalService.withdraw(userId);

            final Reporter author = reportRepository.findById(reportId).orElseThrow().getAuthor();
            assertThat(author.getUserCode()).isNull();
        }
    }
}
