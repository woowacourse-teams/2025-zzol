package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GeminiNicknameAuditorTest {

    private static final List<String> MODELS = List.of("model-1", "model-2", "model-3");

    private Models models;
    private GeminiNicknameAuditor auditor;

    @BeforeEach
    void setUp() {
        final Client client = mock(Client.class);
        models = mock(Models.class);
        ReflectionTestUtils.setField(client, "models", models);

        final NicknameFeedbackRepository feedbackRepository = mock(NicknameFeedbackRepository.class);
        final NicknameAuditPromptTemplate promptTemplate = mock(NicknameAuditPromptTemplate.class);
        given(feedbackRepository.findRecentFeedbacks(any())).willReturn(List.of());
        given(promptTemplate.buildUserMessage(any(), any())).willReturn("PROMPT");

        final NicknameAuditProperties properties =
                new NicknameAuditProperties("api-key", MODELS, 0.85, 100, 20);

        auditor = new GeminiNicknameAuditor(
                client, new ObjectMapper(), properties,
                feedbackRepository, promptTemplate, new SimpleMeterRegistry());
    }

    private static ApiException rateLimit() {
        return new ApiException(429, "RESOURCE_EXHAUSTED", "Quota exceeded");
    }

    @Nested
    class 모델_폴백 {

        @Test
        void 첫_모델이_429면_다음_모델로_폴백한다() throws Exception {
            final GenerateContentResponse ok = mock(GenerateContentResponse.class);
            given(ok.text()).willReturn("[]");
            given(models.generateContent(eq("model-1"), anyString(), any(GenerateContentConfig.class)))
                    .willThrow(rateLimit());
            given(models.generateContent(eq("model-2"), anyString(), any(GenerateContentConfig.class)))
                    .willReturn(ok);

            auditor.audit(List.of("닉네임"));

            verify(models).generateContent(eq("model-1"), anyString(), any(GenerateContentConfig.class));
            verify(models).generateContent(eq("model-2"), anyString(), any(GenerateContentConfig.class));
            verify(models, never()).generateContent(eq("model-3"), anyString(), any(GenerateContentConfig.class));
        }

        @Test
        void 첫_모델이_404면_다음_모델로_폴백한다() throws Exception {
            final GenerateContentResponse ok = mock(GenerateContentResponse.class);
            given(ok.text()).willReturn("[]");
            given(models.generateContent(eq("model-1"), anyString(), any(GenerateContentConfig.class)))
                    .willThrow(new ApiException(404, "NOT_FOUND", "model not found"));
            given(models.generateContent(eq("model-2"), anyString(), any(GenerateContentConfig.class)))
                    .willReturn(ok);

            auditor.audit(List.of("닉네임"));

            verify(models).generateContent(eq("model-1"), anyString(), any(GenerateContentConfig.class));
            verify(models).generateContent(eq("model-2"), anyString(), any(GenerateContentConfig.class));
        }

        @Test
        void 한도_초과가_아닌_오류는_폴백하지_않고_즉시_실패한다() throws Exception {
            given(models.generateContent(eq("model-1"), anyString(), any(GenerateContentConfig.class)))
                    .willThrow(new ApiException(400, "INVALID_ARGUMENT", "bad request"));

            assertThatThrownBy(() -> auditor.audit(List.of("닉네임")))
                    .isInstanceOf(InfrastructureException.class)
                    .hasFieldOrPropertyWithValue("errorCode", NicknameAuditErrorCode.AI_CALL_FAILED);

            verify(models, never()).generateContent(eq("model-2"), anyString(), any(GenerateContentConfig.class));
        }

        @Test
        void 모든_모델이_429면_한도_소진_예외를_던진다() throws Exception {
            given(models.generateContent(anyString(), anyString(), any(GenerateContentConfig.class)))
                    .willThrow(rateLimit());

            assertThatThrownBy(() -> auditor.audit(List.of("닉네임")))
                    .isInstanceOf(InfrastructureException.class)
                    .hasFieldOrPropertyWithValue("errorCode", NicknameAuditErrorCode.AI_RATE_LIMIT_EXHAUSTED);

            verify(models, times(3)).generateContent(anyString(), anyString(), any(GenerateContentConfig.class));
        }
    }

    @Nested
    class 폴백_대상_판별 {

        @Test
        void ApiException_코드가_429면_폴백_대상이다() {
            assertThat(auditor.shouldFallback(rateLimit())).isTrue();
        }

        @Test
        void ApiException_코드가_404면_폴백_대상이다() {
            assertThat(auditor.shouldFallback(new ApiException(404, "NOT_FOUND", "model not found"))).isTrue();
        }

        @Test
        void 그_외_상태코드의_ApiException은_폴백하지_않는다() {
            assertThat(auditor.shouldFallback(new ApiException(400, "INVALID_ARGUMENT", "bad"))).isFalse();
        }

        @Test
        void 원인_체인에_숨은_429도_판별한다() {
            final Throwable wrapped = new RuntimeException("wrap", rateLimit());
            assertThat(auditor.shouldFallback(wrapped)).isTrue();
        }

        @Test
        void RESOURCE_EXHAUSTED_메시지면_타입이_달라도_판별한다() {
            assertThat(auditor.shouldFallback(new RuntimeException("RESOURCE_EXHAUSTED: quota"))).isTrue();
        }

        @Test
        void NOT_FOUND_메시지면_타입이_달라도_판별한다() {
            assertThat(auditor.shouldFallback(new RuntimeException("models/gemini-x is NOT_FOUND"))).isTrue();
        }

        @Test
        void 무관한_예외는_폴백하지_않는다() {
            assertThat(auditor.shouldFallback(new RuntimeException("connection reset"))).isFalse();
        }
    }
}
