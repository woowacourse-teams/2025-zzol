package coffeeshout.user.domain.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.config.UserCodeProperties;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserCodeGenerator {

    private final UserRepository userRepository;
    private final int maxRetry;
    private final Counter generationSuccessCounter;
    private final Counter duplicationCounter;
    private final Counter maxRetryExceededCounter;

    public UserCodeGenerator(
            UserRepository userRepository,
            UserCodeProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.userRepository = userRepository;
        this.maxRetry = properties.maxRetry();
        this.generationSuccessCounter = Counter.builder("userCode.generation.success")
                .description("UserCode 생성 성공 횟수")
                .register(meterRegistry);
        this.duplicationCounter = Counter.builder("userCode.generation.duplication")
                .description("UserCode 생성 중복 발생 횟수")
                .register(meterRegistry);
        this.maxRetryExceededCounter = Counter.builder("userCode.generation.max_retry_exceeded")
                .description("UserCode 생성 최대 재시도 횟수 초과")
                .register(meterRegistry);
    }

    public UserCode generate() {
        for (int attempt = 0; attempt < maxRetry; attempt++) {
            final UserCode candidate = UserCode.generate();

            if (!userRepository.existsByUserCode(candidate)) {
                generationSuccessCounter.increment();
                log.debug("UserCode 생성 성공: {} (시도 횟수: {})", candidate.value(), attempt + 1);
                return candidate;
            }

            duplicationCounter.increment();
            log.debug("UserCode 중복 발생: {} (재시도 중... {}/{})", candidate.value(), attempt + 1, maxRetry);
        }

        maxRetryExceededCounter.increment();
        throw new BusinessException(UserErrorCode.USER_CODE_GENERATION_FAILED,
                "사용자 식별 코드 생성이 실패했습니다. 최대 시도 횟수를 초과했습니다.");
    }
}
