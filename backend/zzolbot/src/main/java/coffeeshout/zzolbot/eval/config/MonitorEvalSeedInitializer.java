package coffeeshout.zzolbot.eval.config;

import coffeeshout.zzolbot.eval.domain.MonitorScenarioFixture;
import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import coffeeshout.zzolbot.eval.domain.ScenarioSource;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.EvalScenarioRepository;
import coffeeshout.zzolbot.eval.infra.MonitorFixtureCodec;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * 알림 분석(MONITOR) 골든 시나리오 시드를 최초 1회 적재한다.
 * 챗봇 시드({@code eval/seed/})와 파일 스키마가 달라 디렉터리를 분리한다 — 같은 글롭에 두면
 * 한쪽 로더가 다른 쪽 파일을 파싱하다 조용히 스킵하는 사고가 난다.
 * 동일 name 시나리오가 아직 없을 때만 삽입하고, 출처가 없으면 MANUAL로 분류한다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MonitorEvalSeedInitializer implements ApplicationRunner {

    private static final String SEED_LOCATION = "classpath:eval/monitor-seed/*.json";

    private final EvalScenarioRepository scenarioRepository;
    private final MonitorFixtureCodec codec;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            final Resource[] resources = new PathMatchingResourcePatternResolver().getResources(SEED_LOCATION);
            for (Resource resource : resources) {
                loadOne(resource);
            }
        } catch (Exception e) {
            log.warn("[ZzolBot] 모니터 평가 시드 적재 실패", e);
        }
    }

    private void loadOne(Resource resource) {
        try {
            final SeedFile seed = objectMapper.readValue(resource.getInputStream(), SeedFile.class);
            if (scenarioRepository.existsByName(seed.name())) {
                return;
            }
            final MonitorScenarioFixture fixture =
                    new MonitorScenarioFixture(seed.alert(), seed.logSamples(), seed.logEnvironment());
            scenarioRepository.save(EvalScenarioEntity.create(
                    seed.name(),
                    ScenarioKind.MONITOR,
                    seed.question(),
                    codec.toJson(fixture),
                    seed.rubric(),
                    resolveSource(seed.source())));
            log.info("[ZzolBot] 모니터 평가 시드 적재: {}", seed.name());
        } catch (Exception e) {
            log.warn("[ZzolBot] 모니터 평가 시드 파일 처리 실패. resource={}", resource.getFilename(), e);
        }
    }

    private ScenarioSource resolveSource(String source) {
        if (source == null || source.isBlank()) {
            return ScenarioSource.MANUAL;
        }
        try {
            return ScenarioSource.valueOf(source.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ScenarioSource.MANUAL;
        }
    }

    private record SeedFile(
            String name,
            String question,
            String rubric,
            String source,
            FiringAlert alert,
            List<String> logSamples,
            String logEnvironment) {}
}
