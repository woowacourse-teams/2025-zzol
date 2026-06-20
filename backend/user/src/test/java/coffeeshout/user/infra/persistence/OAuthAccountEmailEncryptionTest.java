package coffeeshout.user.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.UserModuleIntegrationTest;
import coffeeshout.fixture.UserFixture;
import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class OAuthAccountEmailEncryptionTest extends UserModuleIntegrationTest {

    private static final String PLAIN_EMAIL = "mj@example.com";

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Nested
    class 이메일_암호화_저장 {

        @Test
        void DB에는_평문이_아닌_암호문이_저장된다() {
            userRepository.save(UserFixture.회원_엠제이());

            final String storedEmail = jdbcTemplate.queryForObject(
                    "SELECT email FROM oauth_account LIMIT 1", String.class);

            assertThat(storedEmail).isNotEqualTo(PLAIN_EMAIL);
        }

        @Test
        void JPA로_조회하면_평문이_복호화되어_반환된다() {
            userRepository.save(UserFixture.회원_엠제이());

            final User found = userRepository
                    .findByProviderAndProviderUserId("google", "google-uid-1")
                    .orElseThrow();

            assertThat(found.getOAuthAccount().email()).isEqualTo(PLAIN_EMAIL);
        }
    }

    @Nested
    class 블라인드_인덱스_조회 {

        @Test
        void 이메일로_사용자를_조회할_수_있다() {
            final User saved = userRepository.save(UserFixture.회원_엠제이());

            final User found = userRepository.findByEmail(PLAIN_EMAIL).orElseThrow();

            assertThat(found.getId()).isEqualTo(saved.getId());
        }

        @Test
        void 대소문자가_달라도_같은_사용자가_조회된다() {
            final User saved = userRepository.save(UserFixture.회원_엠제이());

            final User found = userRepository.findByEmail("  MJ@Example.COM  ").orElseThrow();

            assertThat(found.getId()).isEqualTo(saved.getId());
        }

        @Test
        void 존재하지_않는_이메일은_빈_결과를_반환한다() {
            userRepository.save(UserFixture.회원_엠제이());

            assertThat(userRepository.findByEmail("unknown@example.com")).isEmpty();
        }

        @Test
        void 같은_이메일이_여러_provider에_존재해도_예외_없이_조회된다() {
            userRepository.save(UserFixture.회원_엠제이());
            userRepository.save(new User(
                    null,
                    new UserCode("ZZ9ZZ"),
                    new UserNickname("카카오엠제이"),
                    new OAuthAccount(OAuthProvider.KAKAO, "kakao-uid-mj", PLAIN_EMAIL)
            ));

            assertThat(userRepository.findByEmail(PLAIN_EMAIL)).isPresent();
        }
    }
}
