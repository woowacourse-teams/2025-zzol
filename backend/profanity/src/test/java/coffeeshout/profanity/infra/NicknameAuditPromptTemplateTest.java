package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.infra.persistence.audit.NicknameFeedbackEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NicknameAuditPromptTemplateTest {

    private NicknameAuditPromptTemplate template;

    @BeforeEach
    void setUp() {
        template = new NicknameAuditPromptTemplate(new ObjectMapper());
    }

    @Nested
    class buildUserMessage_메시지_구성 {

        @Test
        void 피드백_예시가_없으면_닉네임_목록만_포함된다() {
            final String message = template.buildUserMessage(List.of("용감한호랑이", "씨발"), List.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(message).contains("용감한호랑이");
                softly.assertThat(message).contains("씨발");
                softly.assertThat(message).doesNotContain("운영자 피드백 기반 추가 예시");
            });
        }

        @Test
        void BLOCKED_피드백은_flagged_true로_포함된다() {
            final NicknameFeedbackEntity feedback = blockedFeedback("욕설닉네임");
            final String message = template.buildUserMessage(List.of("욕설닉네임"), List.of(feedback));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(message).contains("운영자 피드백 기반 추가 예시");
                softly.assertThat(message).contains("true");
            });
        }

        @Test
        void ALLOWED_피드백은_flagged_false로_포함된다() {
            final NicknameFeedbackEntity feedback = allowedFeedback("용감한호랑이");
            final String message = template.buildUserMessage(List.of("용감한호랑이"), List.of(feedback));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(message).contains("운영자 피드백 기반 추가 예시");
                softly.assertThat(message).contains("false");
            });
        }

        @Test
        void 닉네임_목록_섹션이_항상_포함된다() {
            final String message = template.buildUserMessage(List.of("닉네임"), List.of());

            assertThat(message).contains("검열할 닉네임 목록");
        }
    }

    private NicknameFeedbackEntity blockedFeedback(String nickname) {
        return new NicknameFeedbackEntity(nickname, true, AiConfidence.of(0.95),
                NicknameFeedbackEntity.OperatorDecision.BLOCKED, null);
    }

    private NicknameFeedbackEntity allowedFeedback(String nickname) {
        return new NicknameFeedbackEntity(nickname, false, AiConfidence.UNKNOWN,
                NicknameFeedbackEntity.OperatorDecision.ALLOWED, null);
    }
}
