package coffeeshout.nunchi.domain;

import coffeeshout.minigame.domain.MiniGameScore;

/**
 * 눈치게임 점수 — 3계층(SOLO/COLLISION/MISS)을 단일 {@code long}에 밴드 인코딩한다(ADR-0031 N4).
 *
 * <p>{@link MiniGameScore}의 {@code equals}/{@code compareTo}가 모두 {@link #getValue()} 단일
 * {@code long} 기반이고 {@code MiniGameResult}가 동점 판정에 {@code equals()}를 쓰므로, 별도
 * {@code Comparator} 없이 값 하나로 정렬·동점이 모두 결정된다(consistent-with-equals 자동 보장).
 * {@code SpeedTouchScore}(완주/DNF를 {@code DNF_BASE} 오프셋으로 packing)의 선례를 3계층으로 확장한다.
 *
 * <p>오름차순({@code MiniGameResult.fromAscending}, 작을수록 좋음) 기준 밴드:
 * <ul>
 *   <li>SOLO: {@code pressInstantMs} — 빠를수록 작음(좋음), 서로 다른 값</li>
 *   <li>COLLISION: {@code COLLISION_BASE + (TIME_PIVOT - collisionInstantMs)} — 늦게 충돌할수록
 *       작음(덜 나쁨), 같은 그룹은 동일 시각을 공유해 동값(동점)</li>
 *   <li>MISS: {@code MISS_VALUE} 상수 — 전부 동값(동점)이며 최악</li>
 * </ul>
 *
 * <p><b>함정</b>: 정상은 빠를수록 좋지만 충돌은 늦게 충돌할수록 덜 나쁘므로, 충돌 계층만 시각을
 * 역전({@code TIME_PIVOT - instant})한다.
 */
public class NunchiScore extends MiniGameScore {

    // 밴드 분리: 모든 SOLO(<~1e15) < 모든 COLLISION(~1e15) < MISS(1e18). long overflow 없음.
    private static final long COLLISION_BASE = 1_000_000_000_000_000L;   // 1e15
    private static final long MISS_VALUE = 1_000_000_000_000_000_000L;   // 1e18
    // 어떤 epoch ms보다도 큰 역전 기준(약 서기 2286년). collisionInstantMs < TIME_PIVOT 를 전제.
    private static final long COLLISION_TIME_PIVOT = 10_000_000_000_000L; // 1e13

    private final long value;
    private final NunchiTier tier;

    private NunchiScore(long value, NunchiTier tier) {
        this.value = value;
        this.tier = tier;
    }

    /**
     * 정상(단독) 입력. 누른 권위 시각(서버 WS 수신 {@code Instant})이 빠를수록 좋다(작은 값).
     * 서로 다른 시각이면 동점이 아니다.
     */
    public static NunchiScore solo(long pressInstantMillis) {
        return new NunchiScore(pressInstantMillis, NunchiTier.SOLO);
    }

    /**
     * 충돌 실패. 그룹의 충돌(anchor) 시각을 공유해 그룹 안에서는 동점이고, 먼저 충돌(이른 시각)할수록
     * 나쁘다(큰 값).
     */
    public static NunchiScore collision(long collisionInstantMillis) {
        final long banded = COLLISION_BASE + (COLLISION_TIME_PIVOT - collisionInstantMillis);
        return new NunchiScore(banded, NunchiTier.COLLISION);
    }

    /** 미입력(타임아웃) 실패. 모두 동일한 최악 값을 공유해 서로 동점이며 제일 꼴등이다. */
    public static NunchiScore miss() {
        return new NunchiScore(MISS_VALUE, NunchiTier.MISS);
    }

    public NunchiTier getTier() {
        return tier;
    }

    @Override
    public long getValue() {
        return value;
    }
}
