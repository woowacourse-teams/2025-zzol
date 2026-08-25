package coffeeshout.zzolbot.eval.infra;

import coffeeshout.zzolbot.eval.domain.MonitorScenarioFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link MonitorScenarioFixture}를 DB 저장용 JSON 문자열로 직렬화/역직렬화한다.
 */
@Component
@RequiredArgsConstructor
public class MonitorFixtureCodec {

    private final ObjectMapper objectMapper;

    public String toJson(MonitorScenarioFixture fixture) {
        try {
            return objectMapper.writeValueAsString(fixture);
        } catch (Exception e) {
            throw new IllegalStateException("모니터 픽스처 직렬화 실패", e);
        }
    }

    public MonitorScenarioFixture fromJson(String json) {
        try {
            return objectMapper.readValue(json, MonitorScenarioFixture.class);
        } catch (Exception e) {
            throw new IllegalStateException("모니터 픽스처 역직렬화 실패", e);
        }
    }
}
