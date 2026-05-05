package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackEntity.OperatorDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerNameAuditPromptTemplateTest {

    PlayerNameAuditPromptTemplate template;

    @BeforeEach
    void setUp() {
        template = new PlayerNameAuditPromptTemplate(new ObjectMapper());
    }

    @Nested
    class 직렬화_실패 {

        @Test
        void 피드백_예시_직렬화_실패_시_IllegalStateException을_던진다() throws Exception {
            final ObjectMapper mockMapper = mock(ObjectMapper.class);
            when(mockMapper.writeValueAsString(any())).thenThrow(new JsonMappingException(null, "직렬화 실패"));
            final PlayerNameAuditPromptTemplate failTemplate = new PlayerNameAuditPromptTemplate(mockMapper);
            final PlayerNameFeedbackEntity feedback = new PlayerNameFeedbackEntity(
                    "씨발", true, null, OperatorDecision.BLOCKED, "욕설"
            );

            assertThatThrownBy(() -> failTemplate.buildUserMessage(List.of("씨발"), List.of(feedback)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("피드백 예시 직렬화 실패");
        }

        @Test
        void 닉네임_목록_직렬화_실패_시_IllegalStateException을_던진다() throws Exception {
            final ObjectMapper mockMapper = mock(ObjectMapper.class);
            when(mockMapper.writeValueAsString(any())).thenThrow(new JsonMappingException(null, "직렬화 실패"));
            final PlayerNameAuditPromptTemplate failTemplate = new PlayerNameAuditPromptTemplate(mockMapper);

            assertThatThrownBy(() -> failTemplate.buildUserMessage(List.of("닉네임"), List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("닉네임 목록 직렬화 실패");
        }
    }

    @Nested
    class 닉네임_목록_포함 {

        @Test
        void 닉네임이_프롬프트에_포함된다() {
            final String prompt = template.buildUserMessage(List.of("용감한호랑이", "씨발"), List.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(prompt).contains("용감한호랑이");
                softly.assertThat(prompt).contains("씨발");
            });
        }

        @Test
        void 빈_닉네임_목록도_처리된다() {
            final String prompt = template.buildUserMessage(List.of(), List.of());

            assertThat(prompt).contains("검열할 닉네임 목록");
        }
    }

    @Nested
    class 피드백_예시_미주입 {

        @Test
        void 피드백_예시가_없으면_예시_섹션이_포함되지_않는다() {
            final String prompt = template.buildUserMessage(List.of("닉네임"), List.of());

            assertThat(prompt).doesNotContain("운영자 피드백 기반 추가 예시");
        }
    }

    @Nested
    class 피드백_예시_주입 {

        @Test
        void BLOCKED_결정은_flagged_true로_포함된다() {
            final PlayerNameFeedbackEntity blocked = new PlayerNameFeedbackEntity(
                    "씨발", true, null, OperatorDecision.BLOCKED, "욕설"
            );

            final String prompt = template.buildUserMessage(List.of("씨발"), List.of(blocked));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(prompt).contains("운영자 피드백 기반 추가 예시");
                softly.assertThat(prompt).contains("씨발");
                softly.assertThat(prompt).contains("true");
            });
        }

        @Test
        void ALLOWED_결정은_flagged_false로_포함된다() {
            final PlayerNameFeedbackEntity allowed = new PlayerNameFeedbackEntity(
                    "미쳤다", false, null, OperatorDecision.ALLOWED, "일반 감탄사"
            );

            final String prompt = template.buildUserMessage(List.of("미쳤다"), List.of(allowed));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(prompt).contains("운영자 피드백 기반 추가 예시");
                softly.assertThat(prompt).contains("미쳤다");
                softly.assertThat(prompt).contains("false");
            });
        }

        @Test
        void 여러_피드백이_모두_포함된다() {
            final List<PlayerNameFeedbackEntity> feedbacks = List.of(
                    new PlayerNameFeedbackEntity("씨발", true, null, OperatorDecision.BLOCKED, "욕설"),
                    new PlayerNameFeedbackEntity("미쳤다", false, null, OperatorDecision.ALLOWED, "감탄사")
            );

            final String prompt = template.buildUserMessage(List.of("씨발", "미쳤다"), feedbacks);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(prompt).contains("씨발");
                softly.assertThat(prompt).contains("미쳤다");
            });
        }
    }

    @Nested
    class 시스템_지시사항 {

        @Test
        void 검열_기준과_응답_형식이_포함된다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(PlayerNameAuditPromptTemplate.SYSTEM_INSTRUCTION).contains("닉네임 검열 전문가");
                softly.assertThat(PlayerNameAuditPromptTemplate.SYSTEM_INSTRUCTION).contains("JSON 배열");
                softly.assertThat(PlayerNameAuditPromptTemplate.SYSTEM_INSTRUCTION).contains("비속어 판단 기준");
            });
        }

        @Test
        void 사용자_메시지에는_지시사항이_포함되지_않는다() {
            final String userMessage = template.buildUserMessage(List.of("닉네임"), List.of());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(userMessage).doesNotContain("닉네임 검열 전문가");
                softly.assertThat(userMessage).doesNotContain("비속어 판단 기준");
            });
        }
    }

    @Nested
    class 프롬프트_구조 {

        @Test
        void 닉네임_목록_섹션이_피드백_예시_뒤에_위치한다() {
            final PlayerNameFeedbackEntity feedback = new PlayerNameFeedbackEntity(
                    "씨발", true, null, OperatorDecision.BLOCKED, "욕설"
            );

            final String prompt = template.buildUserMessage(List.of("테스트닉네임"), List.of(feedback));

            final int feedbackIndex = prompt.indexOf("운영자 피드백 기반 추가 예시");
            final int nicknameListIndex = prompt.indexOf("검열할 닉네임 목록");
            assertThat(feedbackIndex).isLessThan(nicknameListIndex);
        }
    }
}
