package coffeeshout.room.application.service;

import coffeeshout.room.infra.persistence.CustomProfanityJpaRepository;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomProfanityLoader implements ApplicationRunner {

    private final CustomProfanityJpaRepository customProfanityRepository;
    private final BadWordFiltering badWordFiltering;

    @Override
    public void run(ApplicationArguments args) {
        var words = customProfanityRepository.findAll();
        words.forEach(entity -> badWordFiltering.add(entity.getWord()));
        log.info("커스텀 비속어 {}건 로딩 완료", words.size());
    }
}
