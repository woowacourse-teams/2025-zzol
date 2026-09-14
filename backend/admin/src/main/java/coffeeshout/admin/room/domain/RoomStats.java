package coffeeshout.admin.room.domain;

import coffeeshout.admin.overview.domain.GamePlayStat;
import coffeeshout.room.domain.RoomState;
import java.util.List;

/**
 * 방 화면 상단의 그래프 재료.
 *
 * <p>목록만 있는 화면은 "이 방이 어땠나"에만 답한다. 운영이 실제로 묻는 것은 그 위에
 * 하나 더 있다. <b>요즘 방들이 어떤 모양인가</b>. 2인 방이 절반이면 게임 밸런스를 2인
 * 기준으로 봐야 하고, 끝까지 가는 방이 30%면 목록을 아무리 뒤져도 그 사실은 안 보인다.
 *
 * @param roomCount       기간 안에 만들어진 방
 * @param games           게임별 시작 판 수와 완료 판 수. 둘의 차이가 중간에 깨진 판이다
 * @param playerBuckets   인원수 구간별 방 수
 * @param statuses        상태별 방 수. 어디까지 갔는지를 본다
 * @param durationBuckets 소요 시간 구간별 방 수. 끝난 방만 센다
 * @param soloRoomCount   참여자가 방장 하나뿐인 방. 만들어졌지만 초대가 닿지 않은 방이다
 */
public record RoomStats(
        long roomCount,
        List<GamePlayStat> games,
        List<Bucket> playerBuckets,
        List<StatusCount> statuses,
        List<Bucket> durationBuckets,
        long soloRoomCount) {

    /**
     * 칸의 이름은 사람이 읽는 글자다. 화면이 그 글자로 칸을 찾아 합계를 내면, 라벨을
     * 한 번 다듬는 순간 숫자가 조용히 0이 된다. 요약 수는 {@code soloRoomCount} 로 따로 준다.
     */
    public record Bucket(String label, long count) {}

    public record StatusCount(RoomState status, long count) {}
}
