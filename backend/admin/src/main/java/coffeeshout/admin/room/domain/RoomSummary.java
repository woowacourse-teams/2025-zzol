package coffeeshout.admin.room.domain;

import coffeeshout.room.domain.RoomState;
import java.time.LocalDateTime;

/**
 * 방 검색 결과 한 줄.
 *
 * <p>{@code joinCode}는 유니크가 아니다. 같은 코드가 시간이 지나 다른 방에 다시 쓰인다.
 * 그래서 드릴다운은 코드가 아니라 {@code id}로 들어간다.
 */
public record RoomSummary(
        Long id,
        String joinCode,
        RoomState status,
        LocalDateTime createdAt,
        LocalDateTime finishedAt,
        long playerCount) {}
