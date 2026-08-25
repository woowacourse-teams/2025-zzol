package coffeeshout.zzolbot.eval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import coffeeshout.zzolbot.application.ZzolBotChatService;
import coffeeshout.zzolbot.domain.ZzolBotChatResult;
import coffeeshout.zzolbot.eval.domain.ScenarioKind;
import coffeeshout.zzolbot.eval.domain.ScenarioSource;
import coffeeshout.zzolbot.eval.domain.ToolSnapshot;
import coffeeshout.zzolbot.eval.infra.EvalScenarioEntity;
import coffeeshout.zzolbot.eval.infra.ToolSnapshotCodec;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatScenarioEvaluatorTest {

    @Mock
    private ZzolBotChatService chatService;
    @Mock
    private ToolSnapshotCodec codec;

    private ChatScenarioEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ChatScenarioEvaluator(chatService, codec);
    }

    @Test
    void kind는_CHAT이다() {
        assertThat(evaluator.kind()).isEqualTo(ScenarioKind.CHAT);
    }

    @Test
    void 스냅샷을_replay해_챗봇_답변을_산출한다() {
        given(codec.fromJson("[]")).willReturn(new ToolSnapshot(Map.of()));
        given(chatService.ask(eq("질문"), eq("eval"), any(), any(SnapshotToolResultSource.class), any()))
                .willReturn(new ZzolBotChatResult(null, "진단 답변"));
        final EvalScenarioEntity scenario = EvalScenarioEntity.create(
                "chat-1", ScenarioKind.CHAT, "질문", "[]", "rubric", ScenarioSource.MANUAL);

        final EvalAnswer answer = evaluator.evaluate(scenario);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(answer.answer()).isEqualTo("진단 답변");
            softly.assertThat(answer.missingToolCalls()).isZero();
        });
    }
}
