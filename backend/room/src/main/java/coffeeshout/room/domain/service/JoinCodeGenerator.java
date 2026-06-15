package coffeeshout.room.domain.service;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.room.domain.repository.JoinCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JoinCodeGenerator {

    private static final int MAX_RETRY_COUNT = 100;

    private final JoinCodeRepository joinCodeRepository;

    public JoinCode generate() {
        for (int attempt = 0; attempt < MAX_RETRY_COUNT; attempt++) {
            final JoinCode joinCode = JoinCode.generate();

            if (joinCodeRepository.save(joinCode)) {
                log.debug("JoinCode 생성 성공: {} (시도 횟수: {})", joinCode.getValue(), attempt + 1);
                return joinCode;
            }

            log.debug("JoinCode 중복 발생: {} (재시도 중... {}/{})", joinCode.getValue(), attempt + 1, MAX_RETRY_COUNT);
        }

        throw new IllegalStateException("입장 코드 생성이 실패했습니다. 최대 시도 횟수를 초과했습니다.");
    }
}
