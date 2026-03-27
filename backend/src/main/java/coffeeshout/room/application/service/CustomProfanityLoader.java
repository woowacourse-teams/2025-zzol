package coffeeshout.room.application.service;

import coffeeshout.room.domain.service.ProfanityChecker;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomProfanityLoader implements ApplicationRunner {

    private static final int CHUNK_SIZE = 500;

    private final CustomProfanityJpaRepository customProfanityRepository;
    private final ProfanityChecker profanityChecker;

    @Override
    public void run(ApplicationArguments args) {
        int page = 0;
        int totalLoaded = 0;
        Slice<String> slice;

        try {
            slice = customProfanityRepository.findWords(PageRequest.of(page++, CHUNK_SIZE));
        } catch (Exception e) {
            log.error("커스텀 비속어 초기 로딩 실패 — custom_profanity 테이블 조회 중 오류 발생", e);
            throw e;
        }

        do {
            profanityChecker.addAll(slice.getContent());
            totalLoaded += slice.getNumberOfElements();
            if (slice.hasNext()) {
                slice = customProfanityRepository.findWords(PageRequest.of(page++, CHUNK_SIZE));
            }
        } while (slice.hasNext());

        log.info("커스텀 비속어 {}건 로딩 완료", totalLoaded);
    }
}
