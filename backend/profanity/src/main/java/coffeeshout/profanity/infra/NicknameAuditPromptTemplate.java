package coffeeshout.profanity.infra;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameFeedback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NicknameAuditPromptTemplate {

    static final String SYSTEM_INSTRUCTION = """
            너는 한국어 닉네임 검열 전문가다.
            아래 닉네임 목록을 검토하고 각 항목에 대해 JSON 배열로만 응답하라. 반드시 다른 텍스트는 포함하지 마라.

            비속어 판단 기준:
            - 직접적 욕설뿐 아니라 자모 분리, 특수문자 삽입, 유사 발음 대체로 우회한 경우 포함
            - 문화적 맥락을 고려한다 (예: "미쳤다"는 일반 감탄사로 사용되므로 flagged=false)
            - 판단이 애매한 경우 confidence를 낮게 설정한다

            비속어 조각 추출 규칙(terms):
            - flagged=true인 경우, 닉네임에서 비속어에 해당하는 부분 문자열만 terms 배열에 담는다.
            - 정상적인 이름·단어 부분은 절대 포함하지 마라. 예: "경찬이병신" → terms=["병신"] ("경찬이"는 제외)
            - 비속어 조각이 여러 개면 모두 담는다. 예: "시발경찬이병신" → terms=["시발", "병신"]
            - 닉네임 전체가 비속어이면 닉네임 전체를 담는다. 예: "씨발놈" → terms=["씨발놈"]
            - 각 term은 반드시 원본 닉네임에 그대로 등장하는 부분 문자열이어야 한다(새 단어를 만들지 마라).
            - flagged=false이면 terms는 빈 배열([])로 둔다.

            응답 형식:
            [
              { "nickname": "경찬이병신", "flagged": true,  "confidence": 0.97, "reason": "비속어 포함",            "terms": ["병신"] },
              { "nickname": "씨b알",      "flagged": true,  "confidence": 0.97, "reason": "비속어 우회 (특수문자 삽입)", "terms": ["씨b알"] },
              { "nickname": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "일반 닉네임",            "terms": [] }
            ]
            """;

    private static final double EXAMPLE_CONFIDENCE = 0.99;

    private final ObjectMapper objectMapper;

    public String buildUserMessage(List<String> nicknames, List<NicknameFeedback> feedbackExamples) {
        final StringBuilder message = new StringBuilder();
        appendFeedbackExamples(message, feedbackExamples);
        appendNicknameList(message, nicknames);
        return message.toString();
    }

    private void appendFeedbackExamples(StringBuilder message, List<NicknameFeedback> examples) {
        if (examples.isEmpty()) {
            return;
        }
        final List<Map<String, Object>> exampleMaps = examples.stream()
                .map(fb -> {
                    final boolean flagged = fb.getOperatorDecision() == NicknameFeedback.OperatorDecision.BLOCKED;
                    return Map.<String, Object>of(
                            "nickname", fb.getNickname(),
                            "flagged", flagged,
                            "confidence", EXAMPLE_CONFIDENCE,
                            "reason", "운영자 피드백",
                            "terms", flagged ? List.of(fb.getNickname()) : List.of()
                    );
                })
                .toList();
        try {
            message.append("운영자 피드백 기반 추가 예시:\n")
                    .append(objectMapper.writeValueAsString(exampleMaps))
                    .append("\n\n");
        } catch (JsonProcessingException e) {
            throw new InfrastructureException(NicknameAuditErrorCode.PROMPT_BUILD_FAILED, "피드백 예시 직렬화 실패", e);
        }
    }

    private void appendNicknameList(StringBuilder message, List<String> nicknames) {
        try {
            message.append("검열할 닉네임 목록:\n")
                    .append(objectMapper.writeValueAsString(nicknames));
        } catch (JsonProcessingException e) {
            throw new InfrastructureException(NicknameAuditErrorCode.PROMPT_BUILD_FAILED, "닉네임 목록 직렬화 실패", e);
        }
    }
}
