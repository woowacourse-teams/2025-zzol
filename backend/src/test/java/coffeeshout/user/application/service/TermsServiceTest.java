package coffeeshout.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.UserFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TermsServiceTest extends ServiceTest {

    @Autowired
    TermsService termsService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserJpaRepository userJpaRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        final User user = userRepository.save(UserFixture.회원_엠제이());
        userId = user.getId();
    }

    @Nested
    class 약관_동의 {

        @Test
        void 약관_동의_후_termsAgreedAt이_기록된다() {
            termsService.agreeTerms(userId);

            final UserEntity userEntity = userJpaRepository.findById(userId).orElseThrow();
            assertThat(userEntity.getTermsAgreedAt()).isNotNull();
        }

        @Test
        void 약관_동의를_여러_번_호출해도_최신_시각으로_갱신된다() {
            termsService.agreeTerms(userId);
            final UserEntity afterFirst = userJpaRepository.findById(userId).orElseThrow();
            final var firstAgreedAt = afterFirst.getTermsAgreedAt();

            termsService.agreeTerms(userId);
            final UserEntity afterSecond = userJpaRepository.findById(userId).orElseThrow();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(afterSecond.getTermsAgreedAt()).isNotNull();
                softly.assertThat(afterSecond.getTermsAgreedAt()).isAfterOrEqualTo(firstAgreedAt);
            });
        }
    }
}
