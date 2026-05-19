package coffeeshout.room.infra;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.room.domain.audit.PlayerNameAuditErrorCode;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerNameAuditPromptTemplate {

    static final String SYSTEM_INSTRUCTION = """
            너는 한국어 닉네임 검열 전문가다.
            아래 닉네임 목록을 검토하고 각 항목에 대해 JSON 배열로만 응답하라. 반드시 다른 텍스트는 포함하지 마라.

            비속어 판단 기준:
            - 직접적 욕설뿐 아니라 자모 분리, 특수문자 삽입, 유사 발음 대체로 우회한 경우 포함
            - 문화적 맥락을 고려한다 (예: "미쳤다"는 일반 감탄사로 사용되므로 flagged=false)
            - 판단이 애매한 경우 confidence를 낮게 설정한다

            응답 형식:
            [
              { "playerName": "씨b알",      "flagged": true,  "confidence": 0.97, "reason": "비속어 우회 (특수문자 삽입)" },
              { "playerName": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "일반 닉네임" }
            ]
            """;

    private static final double EXAMPLE_CONFIDENCE = 0.99;

    private final ObjectMapper objectMapper;

    public String buildUserMessage(List<String> nicknames, List<PlayerNameFeedbackEntity> feedbackExamples) {
        final StringBuilder message = new StringBuilder();
        appendFeedbackExamples(message, feedbackExamples);
        appendNicknameList(message, nicknames);
        return message.toString();
    }

    private void appendFeedbackExamples(StringBuilder message, List<PlayerNameFeedbackEntity> examples) {
        if (examples.isEmpty()) {
            return;
        }
        final List<Map<String, Object>> exampleMaps = examples.stream()
                .map(fb -> {
                    final boolean flagged = fb.getOperatorDecision() == PlayerNameFeedbackEntity.OperatorDecision.BLOCKED;
                    return Map.<String, Object>of(
                            "playerName", fb.getPlayerName(),
                            "flagged", flagged,
                            "confidence", EXAMPLE_CONFIDENCE,
                            "reason", "운영자 피드백"
                    );
                })
                .toList();
        try {
            message.append("운영자 피드백 기반 추가 예시:\n")
                    .append(objectMapper.writeValueAsString(exampleMaps))
                    .append("\n\n");
        } catch (JsonProcessingException e) {
            throw new InfrastructureException(PlayerNameAuditErrorCode.PROMPT_BUILD_FAILED, "피드백 예시 직렬화 실패", e);
        }
    }

    private void appendNicknameList(StringBuilder message, List<String> nicknames) {
        try {
            message.append("검열할 닉네임 목록:\n")
                    .append(objectMapper.writeValueAsString(nicknames));
        } catch (JsonProcessingException e) {
            throw new InfrastructureException(PlayerNameAuditErrorCode.PROMPT_BUILD_FAILED, "닉네임 목록 직렬화 실패", e);
        }
    }
}
