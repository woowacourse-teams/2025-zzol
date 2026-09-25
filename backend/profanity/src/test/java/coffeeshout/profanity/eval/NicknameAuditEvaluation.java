package coffeeshout.profanity.eval;

import coffeeshout.profanity.eval.GoldenItem.Expected;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 골든셋 한 번 평가한 결과. 흔들림을 뺀 지표는 전부 첫 회차 판정으로 센다.
 *
 * @param confusion   정답 4종 × 판정 4종 건수
 * @param cleanFalsePositive 정상 닉네임을 걸러낸(FLAGGED 또는 PENDING) 비율
 * @param positiveMiss 욕설(PROFANE·EVASION)을 CLEAN으로 넘겼거나 판정을 못 받은 비율
 * @param ambiguousCapture 경계 닉네임을 사람 확인(PENDING)으로 보낸 비율
 * @param termAccuracy 욕설을 FLAGGED로 잡은 건 중 채택된 조각이 정답 조각과 정규화 기준으로 같은 비율
 * @param instability  회차마다 판정이 달라진 닉네임 비율. 1회만 돌리면 n/a다
 * @param mismatches   이상적인 판정과 다른 닉네임. 욕설은 FLAGGED, 경계는 PENDING, 정상은 CLEAN이 이상적이다
 */
public record NicknameAuditEvaluation(
        int itemCount,
        int repeats,
        Scores overall,
        Map<String, Scores> byCategory,
        Map<Expected, Map<Predicted, Integer>> confusion,
        Rate cleanFalsePositive,
        Rate positiveMiss,
        Rate ambiguousCapture,
        Rate termAccuracy,
        Rate instability,
        List<Mismatch> mismatches) {

    public enum Predicted {
        CLEAN,
        FLAGGED,
        PENDING,
        /** 검열기가 이 닉네임의 결과를 돌려주지 않았다. 배치 호출이 실패해도 여기로 센다. */
        MISSING;

        public boolean isPositive() {
            return this == FLAGGED || this == PENDING;
        }
    }

    public record Rate(int hit, int total) {

        @Override
        public String toString() {
            return total == 0 ? "n/a" : "%.1f%% (%d/%d)".formatted(100.0 * hit / total, hit, total);
        }
    }

    /**
     * 경계(AMBIGUOUS)를 뺀 정답으로 잰 정밀도·재현율.
     * positive는 FLAGGED와 PENDING을 모두 걸러낸 것으로 보고, block은 자동 차단되는 FLAGGED만 본다.
     */
    public record Scores(Rate positivePrecision, Rate positiveRecall, Rate blockPrecision, Rate blockRecall) {}

    public record Mismatch(String nickname, String category, Expected expected, Predicted predicted, String reason) {}

    /**
     * @param subject 무엇을 평가했는지. 모델명과 프롬프트 조건처럼 결과만으로는 알 수 없는 것을 적는다.
     */
    public String toMarkdown(String subject) {
        return String.join(
                "\n",
                "# 닉네임 검열 골든셋 평가",
                "",
                "- 대상: " + subject,
                "- 항목 %d건, %d회 반복. 흔들림을 뺀 지표는 첫 회차 기준이다".formatted(itemCount, repeats),
                "",
                summarySection(),
                confusionSection(),
                categorySection(),
                mismatchSection());
    }

    private String summarySection() {
        return String.join(
                "\n",
                "## 요약",
                "",
                "| 지표 | 값 |",
                "| --- | --- |",
                "| 걸러냄 정밀도 (FLAGGED+PENDING) | " + overall.positivePrecision() + " |",
                "| 걸러냄 재현율 (FLAGGED+PENDING) | " + overall.positiveRecall() + " |",
                "| 자동 차단 정밀도 (FLAGGED) | " + overall.blockPrecision() + " |",
                "| 자동 차단 재현율 (FLAGGED) | " + overall.blockRecall() + " |",
                "| 정상어 오탐률 | " + cleanFalsePositive + " |",
                "| 욕설 미탐률 | " + positiveMiss + " |",
                "| 경계 PENDING 포착률 | " + ambiguousCapture + " |",
                "| 조각 정확도 | " + termAccuracy + " |",
                "| 판정 흔들림 | " + instability + " |",
                "");
    }

    private String confusionSection() {
        final String header = Stream.of(Predicted.values()).map(Enum::name).collect(Collectors.joining(" | "));
        final String rows = Stream.of(Expected.values())
                .map(expected -> "| " + expected + " | "
                        + Stream.of(Predicted.values())
                                .map(predicted ->
                                        String.valueOf(confusion.get(expected).get(predicted)))
                                .collect(Collectors.joining(" | "))
                        + " |")
                .collect(Collectors.joining("\n"));
        return String.join(
                "\n",
                "## 혼동행렬 (행: 정답, 열: 판정)",
                "",
                "| 정답 \\ 판정 | " + header + " |",
                "| --- |" + " --- |".repeat(Predicted.values().length),
                rows,
                "");
    }

    private String categorySection() {
        final String rows = byCategory.entrySet().stream()
                .map(entry -> "| %s | %s | %s | %s | %s |"
                        .formatted(
                                entry.getKey(),
                                entry.getValue().positivePrecision(),
                                entry.getValue().positiveRecall(),
                                entry.getValue().blockPrecision(),
                                entry.getValue().blockRecall()))
                .collect(Collectors.joining("\n"));
        return String.join(
                "\n",
                "## 카테고리별",
                "",
                "| 카테고리 | 걸러냄 정밀도 | 걸러냄 재현율 | 자동 차단 정밀도 | 자동 차단 재현율 |",
                "| --- | --- | --- | --- | --- |",
                rows,
                "");
    }

    private String mismatchSection() {
        final String rows = mismatches.stream()
                .map(m -> "| %s | %s | %s | %s | %s |"
                        .formatted(
                                m.nickname(),
                                m.category(),
                                m.expected(),
                                m.predicted(),
                                // 모델이 쓴 사유가 표를 깨지 않게 한다
                                String.valueOf(m.reason()).replace('|', '/').replace('\n', ' ')))
                .collect(Collectors.joining("\n"));
        return String.join(
                "\n",
                "## 이상 판정과 다른 닉네임 (첫 회차, %d건)".formatted(mismatches.size()),
                "",
                "| 닉네임 | 카테고리 | 정답 | 판정 | 사유 |",
                "| --- | --- | --- | --- | --- |",
                rows,
                "");
    }
}
