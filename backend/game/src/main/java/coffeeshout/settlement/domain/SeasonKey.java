package coffeeshout.settlement.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * 시즌 식별자. 월 단위 시즌("2026-07")을 쓴다 — 별도 관리 없이 결정적으로 파생되고,
 * 월이 바뀌면 리더보드가 자연히 초기화되는 가장 단순한 시즌 정책이다.
 * <p>
 * 정산 시각이 아니라 <b>게임 종료(이벤트) 시각</b>에서 파생해야 한다. 재전달된 메시지를
 * 시즌 경계(월말) 이후에 재처리해도 같은 시즌으로 정산되어 멱등성이 유지된다.
 */
public record SeasonKey(String value) {

    private static final ZoneId SEASON_ZONE = ZoneId.of("Asia/Seoul");

    public static SeasonKey from(Instant eventTime) {
        final YearMonth yearMonth = YearMonth.from(eventTime.atZone(SEASON_ZONE));
        return new SeasonKey(yearMonth.toString());
    }
}
