package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.CitationVerifier;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 알림 분석의 프롬프트와 응답 계약. 어느 모델을 쓰든 이 계약은 한 벌이다.
 *
 * <p><b>왜 분리했나.</b> 두 모델의 답을 비교하려면 입력과 채점이 같아야 한다. 프롬프트가 두 벌이면
 * 차이가 모델에서 온 것인지 프롬프트에서 온 것인지 가릴 수 없고, 비교 자체가 무의미해진다. 모델마다
 * 다른 것은 API 호출 방법뿐이고 나머지는 전부 여기 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorAnalysisContract {

    public static final String SYSTEM_INSTRUCTION = """
            너는 운영 모니터링 분석가다. 발화한 알림과 로그 샘플을 보고 아래 JSON으로만 응답하라.
            {
              "summary": "현재 상황 한국어 1~2문장 요약",
              "rootCauseHypothesis": "가장 가능성 높은 근본 원인 가설",
              "suggestedActions": ["운영자가 취할 수 있는 조치 제안", "..."],
              "evidenceFound": true 또는 false,
              "evidenceLine": "evidenceFound가 true일 때 근거가 된 로그 한 줄을 위 샘플에서 그대로 복사"
            }

            근거 판정 규칙 — 이 규칙이 다른 무엇보다 우선한다.
            - 주어진 로그 샘플이 알림 내용과 실제로 관련될 때만 evidenceFound를 true로 둔다.
            - 로그가 알림과 무관하거나, 알림을 설명하지 못하거나, 로그 출처 환경이 알림 대상 환경과
              다르면 evidenceFound를 false로 두고 summary에 "제공된 로그에서 이 알림을 뒷받침할
              근거를 찾지 못했다"는 사실을 명시하라.
            - evidenceFound가 true이면 evidenceLine에 근거가 된 로그 한 줄을 위 샘플에서 한 글자도
              바꾸지 말고 그대로 복사하라. 요약하거나 바꿔 쓰지 마라. 그대로 복사할 로그가 없으면
              evidenceFound는 false다.
            - evidenceFound가 false이면 rootCauseHypothesis는 빈 문자열로 두고 원인을 추측하지 마라.
              suggestedActions도 원인을 전제해서는 안 된다. 근거를 확보하는 조치(알림 시각의 로그를
              다시 조회, 수집 파이프라인 상태 확인)만 제안하고, 로그에 나타나지 않은 컴포넌트나
              시스템을 점검 대상으로 지목하지 마라.
            - 알림 설명(description)에 적힌 원인 가설은 사람이 미리 적어둔 추측일 뿐 확인된 사실이
              아니다. 로그로 뒷받침되지 않으면 그것을 결론으로 삼지 마라.
            - 시간 정합성: 로그 샘플은 알림 발화 직전의 짧은 최근 창(수십 분)에서 조회된 것이다.
              본문 타임스탬프가 그 창에 담길 수 없을 만큼 수 시간에 걸쳐 흩어져 있거나 이전 날짜라면,
              오래된 로그가 일괄 재적재된 아티팩트다. 그런 로그는 근거로 삼지 말고 evidenceFound를
              false로 두고 summary에 타임스탬프 불일치 사실을 명시하라.
            - 양적 정합성: 알림이 말하는 규모(예: 5분에 수십 건)를 로그 샘플이 설명할 수 있는지
              확인하라. 표본이 한두 줄뿐이고 서로 시간이 동떨어진 산발적 에러라면 급증의 근거가
              되지 못한다. evidenceFound를 false로 두라. 단, 샘플이 알림 시각과 정합하는 좁은
              시간대에 몰려 반복된다면 표본 수가 알림 건수보다 적어도 정상적인 근거다.

            없는 수치·테이블·이벤트명을 지어내지 마라. 확인된 것과 추측을 섞지 마라.
            조치는 제안일 뿐 자동 실행되지 않는다. 설명 텍스트 없이 JSON 객체 하나만 출력하라.""";

    /** 근거가 확인되지 않은 분석의 요약을 대체하는 문구. 모델의 단정적 요약이 그대로 남지 않게 한다. */
    private static final String NO_EVIDENCE_SUMMARY = "제공된 로그에서 이 알림을 뒷받침할 근거를 찾지 못했습니다.";

    public String buildPrompt(FiringAlert alert, List<String> logSamples, String logEnvironment) {
        final StringBuilder sb = new StringBuilder();
        sb.append("심각도: ").append(alert.severity()).append('\n');
        sb.append("지문: ").append(alert.fingerprint()).append('\n');
        sb.append("알림명: ").append(alert.alertname()).append('\n');
        sb.append("요약: ").append(alert.summary()).append('\n');
        sb.append("설명(사람이 적어둔 추측 — 확인된 사실 아님): ").append(alert.description()).append('\n');
        sb.append("라벨:\n");
        for (Map.Entry<String, String> label : alert.labels().entrySet()) {
            sb.append("- ")
                    .append(label.getKey())
                    .append('=')
                    .append(label.getValue())
                    .append('\n');
        }
        if (logSamples != null && !logSamples.isEmpty()) {
            sb.append("\n최근 ERROR 로그 샘플 (출처 환경: ").append(logEnvironment).append("):\n");
            for (String line : logSamples) {
                sb.append("- ").append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 모델이 낸 원 응답. 인용 검증을 거치기 <b>전</b>의 값이라 모델의 판별력과 옮겨 적기 능력을
     * 분리해서 볼 수 있다.
     */
    public record ModelAnswer(
            String summary,
            String rootCauseHypothesis,
            List<String> suggestedActions,
            boolean claimedEvidence,
            String evidenceLine) {

        public ModelAnswer {
            suggestedActions = List.copyOf(suggestedActions);
        }
    }

    /**
     * 응답을 한 번만 파싱한다. <b>형식이 깨졌으면 빈 값</b>이라 호출부가 "형식 실패"와
     * "근거 없음이라고 올바르게 답한 것"을 구분할 수 있다.
     *
     * <p>둘을 같은 모양으로 기록하면 형식 실패가 정답으로 집계된다. 비교 기록이 재려는 값이
     * 바로 그것이라 구분이 필요하다.
     */
    public Optional<ModelAnswer> read(String json) {
        try {
            final JsonNode node = TolerantJson.readTree(json);
            final List<String> actions = new ArrayList<>();
            node.path("suggestedActions").forEach(a -> actions.add(a.asText()));
            return Optional.of(new ModelAnswer(
                    node.path("summary").asText(""),
                    node.path("rootCauseHypothesis").asText(""),
                    actions,
                    node.path("evidenceFound").asBoolean(false),
                    node.path("evidenceLine").asText("")));
        } catch (Exception e) {
            log.warn("[ZzolBot] 이상 분석 응답 파싱 실패. raw={}", json, e);
            return Optional.empty();
        }
    }

    /**
     * 원 응답에 인용 검증을 덧씌워 최종 분석으로 만든다. 판정이 누락되면 보수적으로 false로 본다.
     * 근거 있다고 잘못 표시하는 쪽이 더 위험하다.
     *
     * <p>근거가 없으면 모델이 무엇을 보냈든 원인 가설뿐 아니라 요약까지 안전한 문구로 강제한다.
     * 화면엔 "근거 없음"인데 요약은 "DB에 심각한 문제" 같은 단정으로 남는 구멍을 막는다(#1595 리뷰).
     */
    public MonitorAnalysis ground(ModelAnswer answer, List<String> logSamples) {
        final boolean grounded =
                answer.claimedEvidence() && CitationVerifier.citedInLogs(answer.evidenceLine(), logSamples);
        return new MonitorAnalysis(
                grounded ? answer.summary() : NO_EVIDENCE_SUMMARY,
                grounded ? answer.rootCauseHypothesis() : "",
                answer.suggestedActions(),
                grounded);
    }

    /** 읽고 검증까지 한 번에. 형식이 깨졌으면 실패 분석으로 떨어진다. */
    public MonitorAnalysis parse(String json, List<String> logSamples) {
        return read(json).map(answer -> ground(answer, logSamples)).orElseGet(MonitorAnalysis::failed);
    }
}
