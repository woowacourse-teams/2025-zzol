package coffeeshout.admin.user.domain;

import java.time.LocalDateTime;

/**
 * 한 사람의 활동량 한 줄. 분포 그래프의 재료다.
 *
 * <p>구간을 리포지토리가 나누지 않는다. "3회 이상"의 경계는 화면이 무엇을 묻느냐에 따라
 * 바뀌는 값이고, SQL 의 CASE 안에 박아 두면 경계를 옮길 때마다 쿼리를 고치게 된다.
 * 리포지토리는 사람마다 한 줄을 주고, 구간은 서비스가 정한다.
 *
 * @param lastPlayedAt 마지막으로 방에 들어온 때. 한 번도 없으면 null
 */
public record UserPlayAggregate(Long userId, long playCount, LocalDateTime lastPlayedAt) {}
