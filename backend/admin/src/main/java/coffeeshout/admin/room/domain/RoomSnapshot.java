package coffeeshout.admin.room.domain;

import coffeeshout.room.domain.RoomState;
import java.time.LocalDateTime;

/**
 * 분포 그래프를 그리기 위한 방 한 줄.
 *
 * <p>구간을 SQL 이 나누지 않는다. 인원 구간과 소요 시간 구간은 화면이 무엇을 묻느냐에 따라
 * 옮겨지는 값인데, CASE 문에 박아 두면 경계를 바꿀 때마다 쿼리를 고쳐야 하고 세 그래프가
 * 서로 다른 시점의 데이터를 보게 된다. 방 한 줄씩 받아 자바에서 세 번 접는다.
 *
 * <p>기간 안의 방 수만큼 행을 읽는다. 그래서 기간에 상한을 둔다.
 *
 * @param finishedAt 끝나지 않은 방은 null 이다. 소요 시간 분포에서 빠진다
 */
public record RoomSnapshot(RoomState status, LocalDateTime createdAt, LocalDateTime finishedAt, long playerCount) {}
