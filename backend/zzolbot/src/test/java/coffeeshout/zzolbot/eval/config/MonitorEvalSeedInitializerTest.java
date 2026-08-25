package coffeeshout.zzolbot.eval.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.zzolbot.eval.domain.MonitorScenarioFixture;
import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.EvalScenarioRepository;
import coffeeshout.zzolbot.eval.infra.MonitorFixtureCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 실제 classpath의 monitor-seed JSON을 파싱해 적재하는지 검증한다 —
 * 시드 파일 스키마가 깨지면 이 테스트가 잡는다(로더는 파싱 실패를 warn으로 삼키므로).
 */
@ExtendWith(MockitoExtension.class)
class MonitorEvalSeedInitializerTest {

    private static final int SEED_COUNT = 5;

    @Mock
    private EvalScenarioRepository scenarioRepository;

    private MonitorFixtureCodec codec;
    private MonitorEvalSeedInitializer initializer;

    @BeforeEach
    void setUp() {
        final ObjectMapper objectMapper = new ObjectMapper();
        codec = new MonitorFixtureCodec(objectMapper);
        initializer = new MonitorEvalSeedInitializer(scenarioRepository, codec, objectMapper);
    }

    @Test
    void 모든_모니터_시드를_MONITOR_시나리오로_적재한다() {
        given(scenarioRepository.existsByName(anyString())).willReturn(false);

        initializer.run(null);

        final ArgumentCaptor<EvalScenarioEntity> captor = ArgumentCaptor.forClass(EvalScenarioEntity.class);
        verify(scenarioRepository, times(SEED_COUNT)).save(captor.capture());
        SoftAssertions.assertSoftly(softly -> {
            for (EvalScenarioEntity saved : captor.getAllValues()) {
                softly.assertThat(saved.getKind()).isEqualTo(ScenarioKind.MONITOR);
                softly.assertThat(saved.getQuestion()).isNotBlank();
                softly.assertThat(saved.getRubric()).isNotBlank();
                final MonitorScenarioFixture fixture = codec.fromJson(saved.getSnapshotJson());
                softly.assertThat(fixture.alert().alertname()).isNotBlank();
                softly.assertThat(fixture.logSamples()).isNotEmpty();
                softly.assertThat(fixture.logEnvironment()).isEqualTo("prod");
            }
            final List<String> names = captor.getAllValues().stream().map(EvalScenarioEntity::getName).toList();
            softly.assertThat(names).contains(
                    "monitor-ip-ban-unrelated-cardgame-errors",
                    "monitor-ip-blocking-true-scanner-evidence");
        });
    }

    @Test
    void 이미_존재하는_이름의_시드는_건너뛴다() {
        given(scenarioRepository.existsByName(anyString())).willReturn(true);

        initializer.run(null);

        verify(scenarioRepository, times(0)).save(any());
    }
}
