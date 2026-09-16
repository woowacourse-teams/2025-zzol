package coffeeshout.admin.user.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * 유저 화면 상단의 그래프 재료.
 *
 * <p>한 응답으로 묶었다. 넷 다 같은 화면이 같은 순간에 쓰고 갱신 주기도 같아서, 나누면
 * 화면이 네 번 왕복하는 값만 치른다. 갱신 주기가 다른 것을 묶지 않는다는 규칙의 반대편이다.
 *
 * @param userCount       활성 회원 수. 제공자 연결 수의 합과 다르다. 한 사람이 여럿을 연결한다
 * @param providers       제공자별 연결 수
 * @param signups         일자별 가입 수. 가입이 없는 날도 0으로 채워 보낸다
 * @param playBuckets     플레이 횟수 구간별 회원 수. 얼마나 깊이 쓰는지를 본다
 * @param activityBuckets 마지막 플레이 시점 구간별 회원 수. 얼마나 남아 있는지를 본다
 * @param playedUserCount 한 판이라도 끝낸 회원 수
 * @param activeUserCount 최근 7일 안에 방에 들어온 회원 수
 */
public record UserStats(
        long userCount,
        List<ProviderCount> providers,
        List<DailyCount> signups,
        List<Bucket> playBuckets,
        List<Bucket> activityBuckets,
        long playedUserCount,
        long activeUserCount) {

    public record DailyCount(LocalDate date, long count) {}

    /**
     * 분포 한 칸.
     *
     * <p>구간을 서버가 정한다. 화면이 나누면 같은 경계를 두 곳에서 관리하게 되고, 경계가
     * 어긋나면 칸의 합이 회원 수와 안 맞는다.
     */
    public record Bucket(String label, long count) {}

    /**
     * 칸의 이름은 사람이 읽는 글자다. 화면이 그 글자로 칸을 찾아 합계를 내면, 라벨을
     * 한 번 다듬는 순간 숫자가 조용히 0이 된다. 그래서 요약 수는 위 두 필드로 따로 보낸다.
     */
    public static final int ACTIVE_DAYS = 7;
}
