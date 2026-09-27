package coffeeshout.zzolbot.monitor.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShadowAnomalyAnalyzerTest {

    private static final FiringAlert ALERT =
            new FiringAlert("AppErrorLogSpike", "warning", "fp-1", "ERROR 급증", "임계 초과", Map.of("job", "prod-app"));
    private static final List<String> LOGS = List.of("2026-08-26 ERROR consumer lag 12000");
    private static final MonitorAnalysis AUTHORITATIVE =
            new MonitorAnalysis("컨슈머 지연", "컨슈머가 밀렸다", List.of("스케일 아웃"), true);

    @Mock
    private AnomalyAnalyzer authoritative;

    @Mock
    private ShadowAnalysisRecorder recorder;

    @Mock
    private ExecutorService executor;

    @InjectMocks
    private ShadowAnomalyAnalyzer analyzer;

    @Nested
    class 운영_동작은_바뀌지_않는다 {

        @Test
        void 권위_모델의_분석을_그대로_돌려준다() {
            given(authoritative.analyze(ALERT, LOGS, "prod")).willReturn(AUTHORITATIVE);

            assertThat(analyzer.analyze(ALERT, LOGS, "prod")).isEqualTo(AUTHORITATIVE);
        }

        @Test
        void 섀도우는_다른_스레드로_넘긴다() {
            // 같은 스레드에서 이어 돌리면 자체 모델의 지연이 그대로 Slack 게시 지연이 된다
            given(authoritative.analyze(ALERT, LOGS, "prod")).willReturn(AUTHORITATIVE);

            analyzer.analyze(ALERT, LOGS, "prod");

            then(recorder).should(never()).record(any(), anyList(), anyString(), any());
            then(executor).should().execute(any(Runnable.class));
        }

        @Test
        void 넘긴_작업이_두_모델의_결과를_기록한다() {
            given(authoritative.analyze(ALERT, LOGS, "prod")).willReturn(AUTHORITATIVE);
            final ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);

            analyzer.analyze(ALERT, LOGS, "prod");
            then(executor).should().execute(task.capture());
            task.getValue().run();

            then(recorder).should().record(ALERT, LOGS, "prod", AUTHORITATIVE);
        }
    }

    @Nested
    class 섀도우_실패는_운영으로_전파되지_않는다 {

        @Test
        void 실행기_제출이_거부돼도_권위_결과를_돌려준다() {
            given(authoritative.analyze(ALERT, LOGS, "prod")).willReturn(AUTHORITATIVE);
            willThrow(new RejectedExecutionException("풀 종료")).given(executor).execute(any(Runnable.class));

            assertThat(analyzer.analyze(ALERT, LOGS, "prod")).isEqualTo(AUTHORITATIVE);
        }

        @Test
        void 실행기_제출이_거부돼도_예외가_올라가지_않는다() {
            given(authoritative.analyze(ALERT, LOGS, "prod")).willReturn(AUTHORITATIVE);
            willThrow(new RejectedExecutionException("풀 종료")).given(executor).execute(any(Runnable.class));

            assertThatCode(() -> analyzer.analyze(ALERT, LOGS, "prod")).doesNotThrowAnyException();
        }
    }
}
