package coffeeshout.admin.quality.domain;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.report.domain.ReportCategory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QualityStatisticsRepository {

    NicknameAuditQuality findNicknameAuditQuality(Instant from, Instant to);

    long countPendingReports();

    /** 가장 오래된 미처리 신고의 접수 시각. 없으면 비어 있다. */
    Optional<Instant> findOldestPendingReportCreatedAt();

    /**
     * 기간 안의 신고를 한 줄씩. 카테고리, 게임, 날짜, 소요 시간 분포가 모두 이 한 번의
     * 조회에서 나온다.
     *
     * <p>집계를 네 번 나눠 돌리지 않는 이유는 네 그래프가 <b>같은 순간의 같은 신고들</b>을
     * 말해야 하기 때문이다. 따로 돌리면 그사이 하나가 처리되어 카테고리 합과 날짜 합이
     * 어긋나고, 그 어긋남은 화면에서 알아채기 어렵다.
     *
     * <p>기간 안의 신고 수만큼 행을 읽는다. 그래서 기간에 상한을 둔다.
     */
    List<ReportRecord> findReportsBetween(Instant from, Instant to);

    /** 기간 안의 닉네임 검열을 한 줄씩. 위와 같은 이유로 한 번에 읽는다. */
    List<AuditRecord> findAuditsBetween(Instant from, Instant to);

    /**
     * @param gameType   게임과 무관한 신고는 null
     * @param resolvedAt 아직 처리 안 됐으면 null
     */
    record ReportRecord(ReportCategory category, MiniGameType gameType, Instant createdAt, Instant resolvedAt) {}

    /** @param confidence AI 신뢰도 0.0 ~ 1.0. 검열을 안 거친 닉네임은 null */
    record AuditRecord(NicknameAuditStatus status, AiConfidence confidence, Instant createdAt) {}
}
