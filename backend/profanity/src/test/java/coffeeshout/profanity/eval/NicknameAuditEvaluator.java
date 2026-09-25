package coffeeshout.profanity.eval;

import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import coffeeshout.profanity.eval.GoldenItem.Expected;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.Mismatch;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.Predicted;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.Rate;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.Scores;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 골든셋을 검열기에 돌려 정답과 비교한다. 정답 비교만 하고 LLM 채점은 하지 않는다.
 */
@Slf4j
@RequiredArgsConstructor
public class NicknameAuditEvaluator {

    private final TextNormalizer textNormalizer;
    private final int minTermLength;

    public NicknameAuditEvaluation evaluate(
            List<GoldenItem> items, NicknameAuditor auditor, int batchSize, int repeats) {
        final List<Map<String, NicknameAuditResult>> runs = IntStream.range(0, repeats)
                .mapToObj(i -> runOnce(items, auditor, batchSize))
                .toList();
        final List<Judged> judged = items.stream()
                .map(item -> new Judged(item, runs.getFirst().get(item.nickname())))
                .toList();
        final List<Judged> positives = filter(judged, j -> j.item().expected().isPositive());

        return new NicknameAuditEvaluation(
                items.size(),
                repeats,
                scores(judged),
                judged.stream()
                        .collect(Collectors.groupingBy(
                                j -> j.item().category(),
                                LinkedHashMap::new,
                                Collectors.collectingAndThen(Collectors.toList(), this::scores))),
                confusion(judged),
                rate(filter(judged, j -> j.item().expected() == Expected.CLEAN), j -> j.predicted()
                        .isPositive()),
                rate(positives, j -> !j.predicted().isPositive()),
                rate(
                        filter(judged, j -> j.item().expected() == Expected.AMBIGUOUS),
                        j -> j.predicted() == Predicted.PENDING),
                rate(filter(positives, j -> j.predicted() == Predicted.FLAGGED), this::termsMatch),
                instability(items, runs),
                mismatches(judged));
    }

    private Map<String, NicknameAuditResult> runOnce(List<GoldenItem> items, NicknameAuditor auditor, int batchSize) {
        final List<String> nicknames = items.stream().map(GoldenItem::nickname).toList();
        final Map<String, NicknameAuditResult> byNickname = new HashMap<>();
        for (int from = 0; from < nicknames.size(); from += batchSize) {
            final List<String> batch = nicknames.subList(from, Math.min(from + batchSize, nicknames.size()));
            // 응답에 섞인 다른 배치 닉네임이 뒤 항목의 판정을 가로채지 않게 요청한 닉네임만 받는다
            // HashSet이라 모델이 nickname을 null로 돌려줘도 contains가 터지지 않는다
            final Set<String> requested = new HashSet<>(batch);
            auditBatch(auditor, batch).stream()
                    .filter(result -> requested.contains(result.nickname()))
                    .forEach(result -> byNickname.putIfAbsent(result.nickname(), result));
        }
        return byNickname;
    }

    private List<NicknameAuditResult> auditBatch(NicknameAuditor auditor, List<String> batch) {
        try {
            return auditor.audit(batch);
        } catch (RuntimeException e) {
            // 한 배치 실패로 평가 전체를 버리지 않는다. 결과를 못 받은 닉네임은 MISSING으로 센다.
            log.warn("[GoldenEval] 배치 검열 실패. {}건을 MISSING으로 센다", batch.size(), e);
            return List.of();
        }
    }

    private Scores scores(List<Judged> judged) {
        final List<Judged> labeled = filter(judged, j -> j.item().expected() != Expected.AMBIGUOUS);
        final List<Judged> positives = filter(labeled, j -> j.item().expected().isPositive());
        final Predicate<Judged> expectedPositive = j -> j.item().expected().isPositive();
        return new Scores(
                rate(filter(labeled, j -> j.predicted().isPositive()), expectedPositive),
                rate(positives, j -> j.predicted().isPositive()),
                rate(filter(labeled, j -> j.predicted() == Predicted.FLAGGED), expectedPositive),
                rate(positives, j -> j.predicted() == Predicted.FLAGGED));
    }

    private Map<Expected, Map<Predicted, Integer>> confusion(List<Judged> judged) {
        final Map<Expected, Map<Predicted, Integer>> matrix = new EnumMap<>(Expected.class);
        for (final Expected expected : Expected.values()) {
            final Map<Predicted, Integer> row = new EnumMap<>(Predicted.class);
            for (final Predicted predicted : Predicted.values()) {
                row.put(predicted, 0);
            }
            matrix.put(expected, row);
        }
        judged.forEach(j -> matrix.get(j.item().expected()).merge(j.predicted(), 1, Integer::sum));
        return matrix;
    }

    private boolean termsMatch(Judged judged) {
        final Set<String> actual = judged.result().extractProfanityFragments(textNormalizer, minTermLength).stream()
                .map(textNormalizer::normalize)
                .collect(Collectors.toSet());
        final Set<String> expected = judged.item().expectedTerms().stream()
                .map(textNormalizer::normalize)
                .collect(Collectors.toSet());
        return actual.equals(expected);
    }

    private Rate instability(List<GoldenItem> items, List<Map<String, NicknameAuditResult>> runs) {
        if (runs.size() < 2) {
            return new Rate(0, 0);
        }
        final long changed = items.stream()
                .filter(item -> runs.stream()
                                .map(run -> predict(run.get(item.nickname())))
                                .distinct()
                                .count()
                        > 1)
                .count();
        return new Rate(Math.toIntExact(changed), items.size());
    }

    private List<Mismatch> mismatches(List<Judged> judged) {
        return judged.stream()
                .filter(j -> j.predicted() != ideal(j.item().expected()))
                .map(j -> new Mismatch(
                        j.item().nickname(),
                        j.item().category(),
                        j.item().expected(),
                        j.predicted(),
                        j.result() == null ? "응답 없음" : j.result().reason()))
                .toList();
    }

    private static Predicted ideal(Expected expected) {
        return switch (expected) {
            case PROFANE, EVASION -> Predicted.FLAGGED;
            case AMBIGUOUS -> Predicted.PENDING;
            case CLEAN -> Predicted.CLEAN;
        };
    }

    private static Predicted predict(NicknameAuditResult result) {
        if (result == null) {
            return Predicted.MISSING;
        }
        return switch (result.status()) {
            case FLAGGED -> Predicted.FLAGGED;
            case PENDING -> Predicted.PENDING;
            case CLEAN -> Predicted.CLEAN;
            // 검열기는 위 셋만 돌려준다. 그 밖의 상태는 판정을 못 받은 것으로 센다.
            default -> Predicted.MISSING;
        };
    }

    private static List<Judged> filter(List<Judged> judged, Predicate<Judged> condition) {
        return judged.stream().filter(condition).toList();
    }

    private static Rate rate(List<Judged> pool, Predicate<Judged> hit) {
        return new Rate(Math.toIntExact(pool.stream().filter(hit).count()), pool.size());
    }

    private record Judged(GoldenItem item, NicknameAuditResult result) {

        Predicted predicted() {
            return predict(result);
        }
    }
}
