package coffeeshout.global.zzolbot.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public record AskContext(Instant asOf, long seed, String requestId, PiiMaskingSession piiSession) {

    public static AskContext stamp(String question, List<Long> goodIds, Clock clock) {
        final Instant asOf = Instant.now(clock);
        final long seed = computeSeed(question, goodIds, asOf);
        final String requestId = UUID.randomUUID().toString();
        final PiiMaskingSession piiSession = PiiMaskingSession.forSeed(seed);
        return new AskContext(asOf, seed, requestId, piiSession);
    }

    private static long computeSeed(String question, List<Long> goodIds, Instant asOf) {
        final long questionHash = (long) question.hashCode();
        final long idsHash = goodIds.stream().sorted().reduce(0L, (acc, id) -> acc * 31L + id);
        final long timeHash = asOf.truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
        return questionHash * 31L + idsHash * 17L + timeHash;
    }
}
