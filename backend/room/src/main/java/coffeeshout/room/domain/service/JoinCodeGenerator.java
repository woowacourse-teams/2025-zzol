package coffeeshout.room.domain.service;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.repository.JoinCodeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JoinCodeGenerator {

    private static final int MAX_RETRY_COUNT = 100;

    private final JoinCodeRepository joinCodeRepository;
    private final Counter generationSuccessCounter;
    private final Counter duplicationCounter;
    private final Counter maxRetryExceededCounter;

    public JoinCodeGenerator(JoinCodeRepository joinCodeRepository, MeterRegistry meterRegistry) {
        this.joinCodeRepository = joinCodeRepository;
        this.generationSuccessCounter = Counter.builder("joinCode.generation.success")
                .description("JoinCode 생성 성공 횟수")
                .register(meterRegistry);
        this.duplicationCounter = Counter.builder("joinCode.generation.duplication")
                .description("JoinCode 생성 중복 발생 횟수")
                .register(meterRegistry);
        this.maxRetryExceededCounter = Counter.builder("joinCode.generation.max_retry_exceeded")
                .description("JoinCode 생성 최대 재시도 횟수 초과")
                .register(meterRegistry);
    }

    public JoinCode generate() {
        for (int attempt = 0; attempt < MAX_RETRY_COUNT; attempt++) {
            final JoinCode joinCode = JoinCode.generate();

            final boolean saved = joinCodeRepository.save(joinCode);

            if (saved) {
                generationSuccessCounter.increment();
                log.debug("JoinCode 생성 성공: {} (시도 횟수: {})", joinCode.getValue(), attempt + 1);
                return joinCode;
            }

            duplicationCounter.increment();
            log.debug("JoinCode 중복 발생: {} (재시도 중... {}/{})", joinCode.getValue(), attempt + 1, MAX_RETRY_COUNT);
        }

        maxRetryExceededCounter.increment();
        throw new IllegalStateException("입장 코드 생성이 실패했습니다. 최대 시도 횟수를 초과했습니다.");
    }
}
