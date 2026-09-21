package coffeeshout.zzolbot.monitor.infra;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShadowAnalysisRecorderTest {

    private static final Instant NOW = Instant.parse("2026-08-26T14:00:00Z");
    private static final FiringAlert ALERT =
            new FiringAlert("AppErrorLogSpike", "warning", "fp-1", "ERROR 급증", "임계 초과", Map.of("job", "prod-app"));
    private static final List<String> LOGS = List.of("2026-08-26 ERROR consumer lag 12000 group=g1");
    private static final MonitorAnalysis AUTHORITATIVE =
            new MonitorAnalysis("컨슈머 지연", "컨슈머가 밀렸다", List.of("스케일 아웃"), true);

    @Mock
    private MonitorShadowRunRepository repository;

    @Mock
    private ShadowModelClient shadowModel;

    private ShadowAnalysisRecorder recorder;

    private ShadowAnalysisRecorder recorderWith(Clock clock) {
        return new ShadowAnalysisRecorder(
                repository, new MonitorAnalysisContract(new ObjectMapper()), shadowModel, clock);
    }

    private MonitorShadowRunEntity captureSaved() {
        final ArgumentCaptor<MonitorShadowRunEntity> captor = ArgumentCaptor.forClass(MonitorShadowRunEntity.class);
        then(repository).should().save(captor.capture());
        return captor.getValue();
    }

    @Nested
    class 두_모델의_답을_같은_잣대로_기록한다 {

        @Test
        void 권위와_섀도우의_판정을_함께_남긴다() {
            given(shadowModel.generate(ALERT, LOGS, "prod")).willReturn("""
                    {"summary":"컨슈머 지연","rootCauseHypothesis":"컨슈머가 밀렸다","suggestedActions":[],
                     "evidenceFound":true,"evidenceLine":"2026-08-26 ERROR consumer lag 12000 group=g1"}""");
            recorder = recorderWith(Clock.fixed(NOW, ZoneOffset.UTC));

            recorder.record(ALERT, LOGS, "prod", AUTHORITATIVE);

            final MonitorShadowRunEntity saved = captureSaved();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getFingerprint()).isEqualTo("fp-1");
                softly.assertThat(saved.getAlertname()).isEqualTo("AppErrorLogSpike");
                softly.assertThat(saved.getLogSampleCount()).isEqualTo(1);
                softly.assertThat(saved.isAuthoritativeEvidenceFound()).isTrue();
                softly.assertThat(saved.isShadowEvidenceFound()).isTrue();
                softly.assertThat(saved.isShadowFailed()).isFalse();
            });
        }

        @Test
        void 접지_전_주장과_접지_후_판정을_구분해_남긴다() {
            // 없는 로그를 인용하면 주장은 참이지만 검증에 걸려 강등된다. 둘이 갈리는 건수가 인용 실패의 크기다
            given(shadowModel.generate(ALERT, LOGS, "prod")).willReturn("""
                    {"summary":"지연","rootCauseHypothesis":"추측","suggestedActions":[],
                     "evidenceFound":true,"evidenceLine":"지어낸 로그 줄"}""");
            recorder = recorderWith(Clock.fixed(NOW, ZoneOffset.UTC));

            recorder.record(ALERT, LOGS, "prod", AUTHORITATIVE);

            final MonitorShadowRunEntity saved = captureSaved();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.isShadowClaimedEvidence()).isTrue();
                softly.assertThat(saved.isShadowEvidenceFound()).isFalse();
                softly.assertThat(saved.getShadowEvidenceLine()).isEqualTo("지어낸 로그 줄");
            });
        }

        @Test
        void 소요_시간을_남긴다() {
            given(shadowModel.generate(ALERT, LOGS, "prod")).willReturn("""
                    {"evidenceFound":false,"evidenceLine":"","suggestedActions":[]}""");
            recorder = recorderWith(new SteppingClock(NOW, 1500));

            recorder.record(ALERT, LOGS, "prod", AUTHORITATIVE);

            SoftAssertions.assertSoftly(softly ->
                    softly.assertThat(captureSaved().getShadowLatencyMs()).isEqualTo(1500L));
        }
    }

    @Nested
    class 섀도우_실패가_운영을_막지_않는다 {

        @Test
        void 응답_형식이_깨지면_실패로_기록한다() {
            // 정상 행으로 남기면 "근거 없음이라고 올바르게 답한 것"과 섞여 정확도가 부풀려진다
            given(shadowModel.generate(ALERT, LOGS, "prod")).willReturn("모델이 그냥 말했다");
            recorder = recorderWith(Clock.fixed(NOW, ZoneOffset.UTC));

            recorder.record(ALERT, LOGS, "prod", AUTHORITATIVE);

            final MonitorShadowRunEntity saved = captureSaved();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.isShadowFailed()).isTrue();
                softly.assertThat(saved.getShadowError()).isEqualTo("자체 모델 응답 형식이 깨졌습니다.");
            });
        }

        @Test
        void 모델_호출이_실패하면_실패로_기록한다() {
            given(shadowModel.generate(any(), anyList(), anyString())).willThrow(new IllegalStateException("연결 거부"));
            recorder = recorderWith(Clock.fixed(NOW, ZoneOffset.UTC));

            recorder.record(ALERT, LOGS, "prod", AUTHORITATIVE);

            final MonitorShadowRunEntity saved = captureSaved();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.isShadowFailed()).isTrue();
                softly.assertThat(saved.getShadowError()).isEqualTo("연결 거부");
                softly.assertThat(saved.isAuthoritativeEvidenceFound()).isTrue();
            });
        }

        @Test
        void 모델_호출이_실패해도_예외를_올리지_않는다() {
            given(shadowModel.generate(any(), anyList(), anyString())).willThrow(new IllegalStateException("연결 거부"));
            recorder = recorderWith(Clock.fixed(NOW, ZoneOffset.UTC));

            assertThatCode(() -> recorder.record(ALERT, LOGS, "prod", AUTHORITATIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        void 기록마저_실패해도_예외를_올리지_않는다() {
            given(shadowModel.generate(any(), anyList(), anyString())).willThrow(new IllegalStateException("연결 거부"));
            willThrow(new RuntimeException("DB 끊김")).given(repository).save(any());
            recorder = recorderWith(Clock.fixed(NOW, ZoneOffset.UTC));

            assertThatCode(() -> recorder.record(ALERT, LOGS, "prod", AUTHORITATIVE))
                    .doesNotThrowAnyException();
        }
    }

    /** 부를 때마다 정해진 만큼 흐르는 시계. 소요 시간 검증용이다. */
    private static final class SteppingClock extends Clock {

        private final Instant start;
        private final long stepMillis;
        private int calls;

        private SteppingClock(Instant start, long stepMillis) {
            this.start = start;
            this.stepMillis = stepMillis;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return start.plusMillis(stepMillis * calls++);
        }
    }
}
