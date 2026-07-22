package coffeeshout.zzolbot.monitor.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeminiAnomalyAnalyzerTest {

    private static final FiringAlert ALERT = new FiringAlert(
            "AppErrorLogSpike", "warning", "fp-1", "ERROR 급증", "임계 초과",
            Map.of("alertname", "AppErrorLogSpike"));
    // 프로덕션 상수(GeminiAnomalyAnalyzer.NO_EVIDENCE_SUMMARY)와 동일해야 한다. private이라 값으로 고정한다.
    private static final String NO_EVIDENCE_SUMMARY = "제공된 로그에서 이 알림을 뒷받침할 근거를 찾지 못했습니다.";

    // client·properties는 callApi 안에서만 쓰이고 그 메서드를 스텁하므로 null로 둔다.
    @Spy
    private GeminiAnomalyAnalyzer analyzer = new GeminiAnomalyAnalyzer(null, null, new ObjectMapper());

    private MonitorAnalysis analyzeWith(String responseJson, List<String> logSamples) {
        final GenerateContentResponse response = mock(GenerateContentResponse.class);
        given(response.text()).willReturn(responseJson);
        doReturn(response).when(analyzer).callApi(anyString(), any(GenerateContentConfig.class));
        return analyzer.analyze(ALERT, logSamples, "dev");
    }

    @Nested
    class 근거_판정 {

        @Test
        void 인용_로그가_샘플에_실재하면_근거를_인정하고_가설을_유지한다() {
            final String json = """
                    {"summary":"컨슈머 지연","rootCauseHypothesis":"컨슈머가 밀렸다",
                     "suggestedActions":["스케일 아웃"],"evidenceFound":true,
                     "evidenceLine":"ERROR consumer lag 12000"}""";

            final MonitorAnalysis result = analyzeWith(json, List.of("2026-07-22 ERROR consumer lag 12000 group=g1"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.evidenceFound()).isTrue();
                softly.assertThat(result.rootCauseHypothesis()).isEqualTo("컨슈머가 밀렸다");
            });
        }

        @Test
        void 모델이_근거있다_해도_인용이_샘플에_없으면_근거없음으로_강등하고_가설을_비운다() {
            // evidenceLine "table orders" 는 로그 어디에도 없다 — 지어낸 근거.
            final String json = """
                    {"summary":"DB 문제로 보임","rootCauseHypothesis":"orders 테이블 잠금",
                     "suggestedActions":["인덱스 점검"],"evidenceFound":true,
                     "evidenceLine":"ERROR lock wait on table orders"}""";

            final MonitorAnalysis result = analyzeWith(json, List.of("2026-07-22 ERROR consumer lag 12000"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.evidenceFound()).isFalse();
                softly.assertThat(result.rootCauseHypothesis()).isEmpty();
                // 근거 없음이면 모델의 단정적 요약("DB 문제로 보임")이 아니라 표준 문구로 대체된다.
                softly.assertThat(result.summary()).isEqualTo(NO_EVIDENCE_SUMMARY);
            });
        }

        @Test
        void 근거없음으로_강등되면_요약도_표준_문구로_대체한다() {
            // 모델이 근거 있다며 단정적 요약을 냈지만 인용은 실재하지 않는다.
            final String json = """
                    {"summary":"서비스에 심각한 장애 발생","rootCauseHypothesis":"커넥션 풀 고갈",
                     "suggestedActions":["재시작"],"evidenceFound":true,
                     "evidenceLine":"ERROR pool exhausted"}""";

            final MonitorAnalysis result = analyzeWith(json, List.of("2026-07-22 ERROR consumer lag 12000"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.evidenceFound()).isFalse();
                softly.assertThat(result.summary())
                        .isEqualTo(NO_EVIDENCE_SUMMARY)
                        .doesNotContain("심각한 장애");
            });
        }

        @Test
        void evidenceFound가_false면_가설을_비운다_규칙3() {
            final String json = """
                    {"summary":"근거 못 찾음","rootCauseHypothesis":"그래도 컨슈머 지연일 듯",
                     "suggestedActions":[],"evidenceFound":false,"evidenceLine":""}""";

            final MonitorAnalysis result = analyzeWith(json, List.of("무관한 로그"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.evidenceFound()).isFalse();
                softly.assertThat(result.rootCauseHypothesis()).isEmpty();
            });
        }

        @Test
        void evidenceFound가_true여도_evidenceLine이_없으면_근거없음으로_본다() {
            final String json = """
                    {"summary":"요약","rootCauseHypothesis":"가설","suggestedActions":[],
                     "evidenceFound":true}""";

            final MonitorAnalysis result = analyzeWith(json, List.of("ERROR something"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.evidenceFound()).isFalse();
                softly.assertThat(result.rootCauseHypothesis()).isEmpty();
            });
        }

        @Test
        void 여러_줄_로그를_개행_대신_공백으로_인용해도_근거로_인정한다() {
            // 로그 한 건이 스택 트레이스(개행·탭 포함)인데 모델은 한 줄로 펴서 인용한다.
            final List<String> logSamples = List.of("ERROR OutOfMemoryError\n\tat com.foo.Bar(Bar.java:42)");
            final String json = """
                    {"summary":"OOM","rootCauseHypothesis":"힙 부족",
                     "suggestedActions":["힙 상향"],"evidenceFound":true,
                     "evidenceLine":"ERROR OutOfMemoryError at com.foo.Bar(Bar.java:42)"}""";

            final MonitorAnalysis result = analyzeWith(json, logSamples);

            assertThat(result.evidenceFound()).isTrue();
        }
    }

    @Nested
    class 파싱_실패 {

        @Test
        void JSON이_아니면_실패_분석을_반환한다() {
            final MonitorAnalysis result = analyzeWith("이건 JSON이 아니다", List.of("ERROR x"));

            assertThat(result.summary()).isEqualTo(MonitorAnalysis.failed().summary());
        }
    }
}
