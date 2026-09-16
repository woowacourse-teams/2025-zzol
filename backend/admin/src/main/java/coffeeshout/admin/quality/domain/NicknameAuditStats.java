package coffeeshout.admin.quality.domain;

import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * 닉네임 검열 화면 상단의 그래프 재료.
 *
 * <p>대기 목록은 지금 손이 필요한 것만 보여준다. 그 옆에 있어야 하는 것은 <b>검열이
 * 어디로 가고 있는가</b>다. 모델이 어제부터 갑자기 많이 잡기 시작했다면 목록이 길어진
 * 것으로만 보이지, 그게 실제로 욕이 늘어서인지 모델이 예민해져서인지는 판정 분포와
 * 신뢰도 분포를 나란히 봐야 갈린다.
 *
 * @param total             기간 안에 들어온 닉네임
 * @param statuses          판정별 수. 하나도 없는 판정도 0으로 보낸다
 * @param daily             일자별 걸린 닉네임과 통과한 닉네임
 * @param confidenceBuckets AI 신뢰도 구간별 수. 걸린 닉네임만 센다
 */
public record NicknameAuditStats(
        long total, List<StatusCount> statuses, List<DailyCount> daily, List<Bucket> confidenceBuckets) {

    public record StatusCount(NicknameAuditStatus status, long count) {}

    /**
     * @param flagged 사람이 봐야 하는 것으로 걸린 닉네임(FLAGGED, PENDING, BLOCKED)
     * @param passed  그대로 지나간 닉네임
     */
    public record DailyCount(LocalDate date, long flagged, long passed) {}

    public record Bucket(String label, long count) {}
}
