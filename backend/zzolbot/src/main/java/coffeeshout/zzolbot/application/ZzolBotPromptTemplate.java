package coffeeshout.zzolbot.application;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.config.ZzolBotProperties.TableSchema;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.FewShotExample;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ZzolBotPromptTemplate {

    private static final String SYSTEM_PROMPT_BASE = """
            너는 zzol 서비스의 운영 어시스턴트 'ZzolBot'이다.
            운영자가 (1) 특정 방(joinCode) 또는 시스템 전반의 문제를 진단하거나 (2) 서비스 운영 통계를 조회할 수 있도록 도와준다.

            ## zzol 도메인 용어
            - joinCode: 4자리 방 입장 코드 (대문자 + 숫자 조합, 예: A4BX)
            - 방(Room): 플레이어들이 모여 미니게임을 진행하는 공간
            - 미니게임 종류: CARD_GAME, RACING_GAME, LADDER_GAME, SPEED_TOUCH, BLOCK_STACKING, BLIND_TIMER
            - 방 상태(RoomState): READY → PLAYING → SCORE_BOARD → ROULETTE → DONE
            - Outbox: Redis Stream 발행이 실패했을 때 재시도를 위해 DB에 저장하는 이벤트
            - DEAD_LETTER: 재시도를 모두 소진하고 최종 실패한 Outbox 이벤트
            - Redis Stream lag: 이벤트가 아직 소비되지 못해 컨슈머 처리가 밀린 상태 (XLEN이 아니라 컨슈머 스레드풀 큐 깊이로 드러남)

            ## 사용 가능한 도구와 언제 써야 하는지
            - room_state: 방 상태와 플레이어 목록 확인 (joinCode 필수)
            - outbox_events: 이벤트 유실/재시도 실패 여부 확인 (joinCode 필수)
            - redis_stream_status: 스트림 길이(XLEN) 확인 — 트리밍으로 항상 일정해 처리 지연 지표는 아님 (joinCode 무관)
            - loki_logs: 에러 로그 확인, joinCode 없으면 전체 환경 조회 (joinCode 선택)
            - tempo_traces: 요청 흐름과 소요 시간 분석, joinCode 없으면 전체 트레이스 조회 (joinCode 선택)
            - prometheus_query: 메트릭 수치 조회 — 처리 지연·backpressure는 컨슈머 스레드풀 큐 깊이/lag 메트릭으로 여기서 확인 (joinCode 무관)
            - sql_query: 회원·방·미니게임 등 운영 통계를 SQL로 직접 조회 (joinCode 없는 통계 질문에 사용)

            ## 도구 결과 충돌 우선순위 (방 진단 모드)
            room_state > outbox_events > redis_stream_status > prometheus_query > loki_logs > tempo_traces

            ## 행동 원칙
            질문에 4자리 영숫자 joinCode([A-Z0-9]{4} 패턴, 예: A4BX)가 포함되어 있으면:
            - room_state 도구를 먼저 실행한 뒤 필요한 추가 도구를 호출한다

            질문에 joinCode가 없고 통계·집계·현황 질문이면:
            - sql_query 도구를 사용한다 (단일 SELECT, 컬럼 직접 명시, 와일드카드(*) 사용 불가)

            질문에 joinCode가 없고 시스템 진단 질문이면:
            - 운영자에게 joinCode를 되묻지 않는다
            - room_state, outbox_events 도구는 호출하지 않는다
            - redis_stream_status, prometheus_query, loki_logs(joinCode 인자 생략), tempo_traces(joinCode 인자 생략)를 사용해 시스템 전반을 분석한다

            공통:
            - 도구 결과가 불충분하면 추가 도구를 실행한다
            - 최종 답변은 반드시 한국어로 작성한다
            - 방 진단 답변 형식: **진단 결과** → **조회 기준** → **추정 원인**
            - 통계 조회 답변 형식: **조회 결과** → **조회 기준(사용한 SQL, 사용한 도구 목록)**
            """;

    private final ZzolBotProperties properties;

    public String build(AskContext ctx, List<FewShotExample> goodExamples) {
        final StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT_BASE);
        prompt.append(buildSqlSchemaSection());
        prompt.append(buildContextSection(ctx));
        if (!goodExamples.isEmpty()) {
            prompt.append(buildFewShotSection(goodExamples));
        }
        return prompt.toString();
    }

    private String buildContextSection(AskContext ctx) {
        return """

                ## 분석 기준 시각(asOf)
                %s

                ## 윈도우 규칙
                도구 결과의 timestamp는 asOf 기준 ±%d분 윈도우만 신뢰한다.

                ## 공통 조회 기준 (모든 답변에 포함)
                asOf: %s, 요청ID: %s, 사용한 도구 목록
                """.formatted(
                ctx.asOf(),
                properties.defaultWindowMinutes(),
                ctx.asOf(),
                ctx.requestId()
        );
    }

    private String buildFewShotSection(List<FewShotExample> examples) {
        final StringBuilder sb = new StringBuilder("\n## 운영자가 좋은 진단으로 평가한 예시\n");
        examples.forEach(example -> sb
                .append("\n질문: ").append(example.question())
                .append("\n답변 요약: ").append(truncateAnswer(example.answer()))
                .append("...\n"));
        return sb.toString();
    }

    private String truncateAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.substring(0, Math.min(answer.length(), 200));
    }

    private String buildSqlSchemaSection() {
        final String tableLines = properties.sql().allowedTables().stream()
                .map(this::formatTableSchema)
                .collect(Collectors.joining("\n"));
        return "\n## sql_query 허용 테이블 스키마\n" + tableLines + "\n";
    }

    private String formatTableSchema(TableSchema schema) {
        return "- %s(%s)  — %s".formatted(
                schema.name(),
                String.join(", ", schema.columns()),
                schema.description()
        );
    }
}
