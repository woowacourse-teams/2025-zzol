package coffeeshout.global.zzolbot.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.AskContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZzolBotPromptTemplateTest {

    private static final ZzolBotProperties PROPERTIES = new ZzolBotProperties(
            "test-key",
            "gemini-2.0-flash",
            8,
            new ZzolBotProperties.MonitoringProperties(
                    "http://loki:3100",
                    "http://tempo:3200",
                    "http://prometheus:9090",
                    "local"
            ),
            new ZzolBotProperties.DeterminismProperties(0.1, 0.1),
            60,
            10000L
    );

    private static final AskContext CTX = AskContext.stamp("test", List.of(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    private ZzolBotPromptTemplate promptTemplate;

    @BeforeEach
    void setUp() {
        promptTemplate = new ZzolBotPromptTemplate(PROPERTIES);
    }

    @Test
    void 핵심_도메인_용어와_도구_설명을_포함한_프롬프트를_반환한다() {
        final String prompt = promptTemplate.build(CTX, List.of());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(prompt).isNotBlank();
            softly.assertThat(prompt).contains("joinCode");
            softly.assertThat(prompt).contains("ZzolBot");
            softly.assertThat(prompt).contains("room_state");
            softly.assertThat(prompt).contains("outbox_events");
            softly.assertThat(prompt).contains("loki_logs");
            softly.assertThat(prompt).contains("redis_stream_status");
            softly.assertThat(prompt).contains("tempo_traces");
            softly.assertThat(prompt).contains("prometheus_query");
            softly.assertThat(prompt).contains("RoomState");
            softly.assertThat(prompt).contains("DEAD_LETTER");
        });
    }

    @Test
    void asOf와_요청ID_및_우선순위_섹션이_포함된다() {
        final String prompt = promptTemplate.build(CTX, List.of());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(prompt).contains("asOf");
            softly.assertThat(prompt).contains(CTX.requestId());
            softly.assertThat(prompt).contains("room_state > outbox_events");
            softly.assertThat(prompt).contains("조회 기준");
        });
    }

    @Test
    void 동일한_ctx와_examples로_빌드하면_동일한_결과를_반환한다() {
        final String prompt1 = promptTemplate.build(CTX, List.of());
        final String prompt2 = promptTemplate.build(CTX, List.of());

        assertThat(prompt1).isEqualTo(prompt2);
    }
}
