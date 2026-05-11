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
import org.junit.jupiter.api.Nested;
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
            10000L,
            new ZzolBotProperties.SqlProperties(
                    List.of(
                            new ZzolBotProperties.TableSchema(
                                    "app_user",
                                    List.of("id", "nickname", "created_at"),
                                    List.of("provider_user_id"),
                                    "회원 정보"
                            )
                    ),
                    100,
                    3
            )
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

    @Test
    void joinCode_있을때와_없을때_행동_원칙이_모두_포함된다() {
        final String prompt = promptTemplate.build(CTX, List.of());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(prompt).contains("joinCode가 없고");
            softly.assertThat(prompt).contains("되묻지 않는다");
            softly.assertThat(prompt).contains("room_state, outbox_events 도구는 호출하지 않는다");
        });
    }

    @Test
    void 도구별_joinCode_의존성이_명시된다() {
        final String prompt = promptTemplate.build(CTX, List.of());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(prompt).contains("joinCode 필수");
            softly.assertThat(prompt).contains("joinCode 선택");
            softly.assertThat(prompt).contains("joinCode 무관");
        });
    }

    @Nested
    class sql_query_도구_스키마_섹션 {

        @Test
        void sql_query_도구와_허용_테이블_스키마가_포함된다() {
            final String prompt = promptTemplate.build(CTX, List.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(prompt).contains("sql_query");
                softly.assertThat(prompt).contains("app_user");
                softly.assertThat(prompt).contains("id, nickname, created_at");
                softly.assertThat(prompt).contains("회원 정보");
            });
        }

        @Test
        void joinCode_없는_통계_질문에_sql_query를_사용하라는_행동_원칙이_포함된다() {
            final String prompt = promptTemplate.build(CTX, List.of());

            assertThat(prompt).contains("통계");
        }
    }
}
