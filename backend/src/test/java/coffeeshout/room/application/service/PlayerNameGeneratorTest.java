package coffeeshout.room.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class NicknameGeneratorTest {

    NicknameGenerator nicknameGenerator;

    @BeforeEach
    void setUp() {
        nicknameGenerator = new NicknameGenerator();
    }

    @Nested
    class 닉네임_생성 {

        @RepeatedTest(20)
        void 생성된_닉네임은_10자_이하다() {
            final String nickname = nicknameGenerator.generate(Set.of());

            assertThat(nickname.length()).isLessThanOrEqualTo(10);
        }

        @RepeatedTest(20)
        void 생성된_닉네임은_기존_목록에_없다() {
            final Set<String> existingNames = Set.of("용감한호랑이", "빠른여우", "조용한판다");

            final String nickname = nicknameGenerator.generate(existingNames);

            assertThat(existingNames).doesNotContain(nickname);
        }

        @Test
        void 기존_닉네임과_충돌하면_다른_닉네임을_생성한다() {
            final String first = nicknameGenerator.generate(Set.of());
            final String second = nicknameGenerator.generate(Set.of(first));

            assertThat(second).isNotEqualTo(first);
        }
    }
}
