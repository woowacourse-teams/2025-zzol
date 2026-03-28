package coffeeshout.room.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.exception.custom.BusinessException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class PlayerNameGeneratorTest {

    @Nested
    class 닉네임_생성 {

        @RepeatedTest(20)
        void 생성된_닉네임은_10자_이하다() {
            WordPicker random = words -> words.get(ThreadLocalRandom.current().nextInt(words.size()));
            final PlayerNameGenerator generator = new PlayerNameGenerator(random);

            final String nickname = generator.generate(Set.of()).value();

            assertThat(nickname.length()).isLessThanOrEqualTo(10);
        }

        @Test
        void 기존_닉네임과_충돌하면_재시도해서_다른_닉네임을_생성한다() {
            // 첫 시도: "용감한" + "호랑이" = 충돌
            // 두 번째 시도: "빠른" + "여우" = 성공
            List<String> sequence = List.of("용감한", "호랑이", "빠른", "여우");
            AtomicInteger idx = new AtomicInteger(0);
            PlayerNameGenerator generator = new PlayerNameGenerator(words -> sequence.get(idx.getAndIncrement()));

            final String nickname = generator.generate(Set.of("용감한호랑이")).value();

            assertThat(nickname).isEqualTo("빠른여우");
        }

        @Test
        void 최대_재시도_횟수를_초과하면_예외를_던진다() {
            // 항상 ADJECTIVES[0] + NOUNS[0] = "용감한호랑이"만 생성
            PlayerNameGenerator generator = new PlayerNameGenerator(List::getFirst);

            assertThatThrownBy(() -> generator.generate(Set.of("용감한호랑이")))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
