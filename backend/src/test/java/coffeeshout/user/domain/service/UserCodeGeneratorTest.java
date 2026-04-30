package coffeeshout.user.domain.service;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.user.config.UserCodeProperties;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserCodeGeneratorTest {

    private UserRepository userRepository;
    private UserCodeGenerator generator;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        final UserCodeProperties properties = new UserCodeProperties(100);
        generator = new UserCodeGenerator(userRepository, properties, new SimpleMeterRegistry());
    }

    @Nested
    class 정상_생성 {

        @Test
        void 중복이_없으면_첫_시도에_코드가_생성된다() {
            given(userRepository.existsByUserCode(any())).willReturn(false);

            final UserCode result = generator.generate();

            assertThat(result).isNotNull();
            assertThat(result.value()).hasSize(5);
            verify(userRepository, times(1)).existsByUserCode(any());
        }

        @Test
        void 처음_두_번_중복이면_세_번째에_성공한다() {
            given(userRepository.existsByUserCode(any()))
                    .willReturn(true)
                    .willReturn(true)
                    .willReturn(false);

            final UserCode result = generator.generate();

            assertThat(result).isNotNull();
            verify(userRepository, times(3)).existsByUserCode(any());
        }
    }

    @Nested
    class 최대_재시도_초과 {

        @Test
        void 모든_시도에서_중복이면_예외가_발생한다() {
            given(userRepository.existsByUserCode(any())).willReturn(true);
            final UserCodeProperties properties = new UserCodeProperties(3);
            final UserCodeGenerator limitedGenerator = new UserCodeGenerator(
                    userRepository, properties, new SimpleMeterRegistry());

            assertCoffeeShoutException(
                    limitedGenerator::generate,
                    UserErrorCode.USER_CODE_GENERATION_FAILED
            );

            verify(userRepository, times(3)).existsByUserCode(any());
        }
    }
}
