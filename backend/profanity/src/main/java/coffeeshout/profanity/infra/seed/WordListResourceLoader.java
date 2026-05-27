package coffeeshout.profanity.infra.seed;

import coffeeshout.profanity.application.ProfanityFilterService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.global.exception.custom.BusinessException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class WordListResourceLoader {

    private static final String KOREAN_RESOURCE = "/profanity/korean-badwords.txt";
    private static final String ENGLISH_RESOURCE = "/profanity/english-badwords.txt";

    private final ProfanityWordRepository wordRepository;
    private final ProfanityFilterService filterService;
    private final TextNormalizer textNormalizer;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void load() {
        int korean = seed(KOREAN_RESOURCE, Language.KOREAN, WordSource.VANE);
        int english = seed(ENGLISH_RESOURCE, Language.ENGLISH, WordSource.LDNOOBW);
        if (korean + english > 0) {
            filterService.rebuildTrie();
        }
        log.info("비속어 단어 시드 완료 — 한국어 {}건, 영어 {}건 신규 등록", korean, english);
    }

    private int seed(String resourcePath, Language language, WordSource source) {
        final InputStream stream = getClass().getResourceAsStream(resourcePath);
        if (stream == null) {
            log.warn("비속어 리소스 파일 없음: {}", resourcePath);
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            final List<ProfanityWord> words = reader.lines()
                    .map(line -> line.replace("﻿", "").trim())
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .flatMap(line -> {
                        try {
                            return Stream.of(ProfanityWord.of(textNormalizer.normalize(line), language, source));
                        } catch (BusinessException e) {
                            log.debug("단어 시드 스킵 ({}): {}", e.getMessage(), line);
                            return Stream.empty();
                        }
                    })
                    .toList();
            return wordRepository.bulkInsertIgnore(words);
        } catch (IOException e) {
            log.error("비속어 리소스 파일 읽기 실패: {}", resourcePath, e);
            return 0;
        }
    }
}
