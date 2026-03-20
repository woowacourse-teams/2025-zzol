package coffeeshout.room.application.service;

import coffeeshout.room.infra.persistence.nickname.CustomProfanityJpaRepository;
import com.vane.badwordfiltering.BadWordFiltering;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomProfanityLoader implements ApplicationRunner {

    private static final int CHUNK_SIZE = 500;

    private final CustomProfanityJpaRepository customProfanityRepository;
    private final BadWordFiltering badWordFiltering;

    @Override
    public void run(ApplicationArguments args) {
        int page = 0;
        int totalLoaded = 0;
        List<String> chunk;

        do {
            chunk = customProfanityRepository.findWords(PageRequest.of(page++, CHUNK_SIZE));
            badWordFiltering.addAll(chunk);
            totalLoaded += chunk.size();
        } while (chunk.size() == CHUNK_SIZE);

        log.info("커스텀 비속어 {}건 로딩 완료", totalLoaded);
    }
}
