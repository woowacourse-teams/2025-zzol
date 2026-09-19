package coffeeshout.zzolbot.monitor.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MonitorAnalysisContractTest {

    private static final List<String> LOGS = List.of("2026-08-26 ERROR consumer lag 12000 group=g1");
    private static final FiringAlert ALERT =
            new FiringAlert("AppErrorLogSpike", "warning", "fp-1", "ERROR 급증", "임계 초과", Map.of("job", "prod-app"));

    private final MonitorAnalysisContract contract = new MonitorAnalysisContract(new ObjectMapper());

    @Nested
    class 접지_전_값을_따로_읽는다 {

        @Test
        void 모델이_주장한_판정을_그대로_읽는다() {
            // 인용 검증에 걸려 강등될 주장도 주장으로 센다. 모델의 판별력을 보는 값이다
            final String json = """
                    {"evidenceFound":true,"evidenceLine":"없는 로그"}""";

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(contract.claimedEvidence(json)).isTrue();
                softly.assertThat(contract.parse(json, LOGS).evidenceFound()).isFalse();
            });
        }

        @Test
        void 인용_원문을_그대로_읽는다() {
            final String json = """
                    {"evidenceFound":true,"evidenceLine":"2026-08-26 ERROR consumer lag 12000 group=g1"}""";

            assertThat(contract.citedLine(json)).isEqualTo("2026-08-26 ERROR consumer lag 12000 group=g1");
        }

        @Test
        void 깨진_JSON이면_주장하지_않은_것으로_본다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(contract.claimedEvidence("{깨짐")).isFalse();
                softly.assertThat(contract.citedLine("{깨짐")).isEmpty();
            });
        }
    }

    @Nested
    class 프롬프트는_한_벌이다 {

        @Test
        void 알림과_로그_샘플이_프롬프트에_들어간다() {
            final String prompt = contract.buildPrompt(ALERT, LOGS, "prod");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(prompt).contains("알림명: AppErrorLogSpike");
                softly.assertThat(prompt).contains("job=prod-app");
                softly.assertThat(prompt).contains("출처 환경: prod");
                softly.assertThat(prompt).contains(LOGS.get(0));
            });
        }

        @Test
        void 로그가_없으면_샘플_절을_넣지_않는다() {
            assertThat(contract.buildPrompt(ALERT, List.of(), "prod")).doesNotContain("최근 ERROR 로그 샘플");
        }
    }

    @Nested
    class 파싱_실패는_안전하게_떨어진다 {

        @Test
        void 응답이_JSON이_아니면_실패_분석을_돌려준다() {
            final MonitorAnalysis result = contract.parse("모델이 그냥 말했다", LOGS);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.evidenceFound()).isFalse();
                softly.assertThat(result.rootCauseHypothesis()).isEmpty();
            });
        }
    }
}
