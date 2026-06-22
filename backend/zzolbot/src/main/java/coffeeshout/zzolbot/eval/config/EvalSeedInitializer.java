package coffeeshout.zzolbot.eval.config;

import coffeeshout.zzolbot.eval.domain.ScenarioSource;
import coffeeshout.zzolbot.eval.domain.ToolCallKey;
import coffeeshout.zzolbot.eval.domain.ToolSnapshot;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.EvalScenarioRepository;
import coffeeshout.zzolbot.eval.infra.ToolSnapshotCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * 콜드 스타트용 골든 시나리오 시드를 최초 1회 적재한다.
 * {@code classpath:eval/seed/*.json}를 읽어 동일 name 시나리오가 아직 없을 때만 삽입한다(중복 방지).
 * 출처는 시드의 {@code source} 필드로 지정하며, 없으면 MANUAL로 분류한다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class EvalSeedInitializer implements ApplicationRunner {

    private static final String SEED_LOCATION = "classpath:eval/seed/*.json";

    private final EvalScenarioRepository scenarioRepository;
    private final ToolSnapshotCodec codec;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            final Resource[] resources = new PathMatchingResourcePatternResolver().getResources(SEED_LOCATION);
            for (Resource resource : resources) {
                loadOne(resource);
            }
        } catch (Exception e) {
            log.warn("[ZzolBot] 평가 시드 적재 실패", e);
        }
    }

    private void loadOne(Resource resource) {
        try {
            final SeedFile seed = objectMapper.readValue(resource.getInputStream(), SeedFile.class);
            if (scenarioRepository.existsByName(seed.name())) {
                return;
            }
            final Map<ToolCallKey, String> results = new LinkedHashMap<>();
            for (SeedEntry entry : seed.snapshot()) {
                results.put(ToolCallKey.of(entry.toolName(), entry.args()), entry.content());
            }
            final String snapshotJson = codec.toJson(new ToolSnapshot(results));
            scenarioRepository.save(EvalScenarioEntity.create(
                    seed.name(), seed.question(), snapshotJson, seed.rubric(), resolveSource(seed.source())));
            log.info("[ZzolBot] 평가 시드 적재: {}", seed.name());
        } catch (Exception e) {
            log.warn("[ZzolBot] 평가 시드 파일 처리 실패. resource={}", resource.getFilename(), e);
        }
    }

    private ScenarioSource resolveSource(String source) {
        if (source == null || source.isBlank()) {
            return ScenarioSource.MANUAL;
        }
        try {
            return ScenarioSource.valueOf(source.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("[ZzolBot] 알 수 없는 시드 source='{}' — MANUAL로 분류", source);
            return ScenarioSource.MANUAL;
        }
    }

    private record SeedFile(String name, String question, String rubric, String source, List<SeedEntry> snapshot) {
    }

    private record SeedEntry(String toolName, Map<String, Object> args, String content) {
    }
}
