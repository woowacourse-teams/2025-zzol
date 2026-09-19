package coffeeshout.admin.room.domain;

import java.time.LocalDateTime;

/**
 * @param winnerProbability 당첨자가 걸릴 확률(%). "왜 내가 걸렸냐"는 문의에 답하는 값이다.
 */
public record RoomRouletteResult(
        Long winnerPlayerId, String winnerPlayerName, Integer winnerProbability, LocalDateTime createdAt) {}
