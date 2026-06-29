package coffeeshout.profanity.infra.seed;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.WordSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WordSeedFileIntegrityTest {

    private final TextNormalizer textNormalizer = new TextNormalizer();

    @Test
    void 시드_파일의_모든_줄이_스킵없이_로드된다() {
        List<String> skipped = new ArrayList<>();
        skipped.addAll(findSkippedLines("/profanity/korean-badwords.txt", Language.KOREAN, WordSource.VANE));
        skipped.addAll(findSkippedLines("/profanity/english-badwords.txt", Language.ENGLISH, WordSource.LDNOOBW));

        // 정규화 후 검증에 걸려 스킵되는 줄(예: 3자 미만 ASCII로 붕괴하는 @!@→aa)은 시드 파일에서
        // 제거해 노이즈 로그와 죽은 데이터를 남기지 않는다. 새 단어 추가 시 이 가드가 회귀를 잡는다.
        assertThat(skipped)
                .as("시드 파일에 로드 불가능한(스킵되는) 줄이 있다: %s", skipped)
                .isEmpty();
    }

    private List<String> findSkippedLines(String resourcePath, Language language, WordSource source) {
        List<String> skipped = new ArrayList<>();
        InputStream stream = getClass().getResourceAsStream(resourcePath);
        assertThat(stream).as("시드 리소스 누락: %s", resourcePath).isNotNull();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String raw;
            while ((raw = reader.readLine()) != null) {
                String line = raw.replace("﻿", "").trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                try {
                    ProfanityWord.of(textNormalizer.normalize(line), language, source);
                } catch (BusinessException e) {
                    skipped.add(resourcePath + " → '" + line + "' (" + e.getErrorCode() + ")");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("시드 파일 읽기 실패: " + resourcePath, e);
        }
        return skipped;
    }
}
