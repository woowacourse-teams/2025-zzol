package coffeeshout.zzolbot.eval.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.zzolbot.eval.domain.MonitorScenarioFixture;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MonitorFixtureCodecTest {

    private final MonitorFixtureCodec codec = new MonitorFixtureCodec(new ObjectMapper());

    @Test
    void 직렬화_역직렬화_왕복이_원본을_보존한다() {
        final MonitorScenarioFixture fixture = new MonitorScenarioFixture(
                new FiringAlert(
                        "IpBanRateSpike", "warning", "fp-1", "요약", "설명", Map.of("incident_group", "ip-blocking")),
                List.of("로그 A", "로그 B"),
                "prod");

        final MonitorScenarioFixture restored = codec.fromJson(codec.toJson(fixture));

        assertThat(restored).isEqualTo(fixture);
    }

    @Test
    void 잘못된_JSON이면_예외를_던진다() {
        assertThatThrownBy(() -> codec.fromJson("not-json")).isInstanceOf(IllegalStateException.class);
    }
}
