package coffeeshout.nunchi.application.response;

import coffeeshout.nunchi.domain.NunchiState;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 눈치게임 상태 브로드캐스트(ADR-0031 결정 8). 상태 머신
 * {@code DESCRIPTION → READY → PLAYING ↔ COLLISION_COOLDOWN → DONE}을 한 DTO로 표현하고,
 * {@code @JsonInclude(NON_NULL)}로 상태별 무관 필드를 JSON에서 제외해 컨트랙트에 정확히 맞춘다.
 * 모든 타이밍은 서버 epoch ms이며 종료(DONE)를 제외한 모든 메시지에 {@code serverNowEpochMs}(스큐 보정)를 싣는다.
 * DONE은 보정할 타이밍이 없어 {@code serverNowEpochMs}를 null로 두므로 {@code @JsonInclude(NON_NULL)}에 의해 필드가 누락된다.
 *
 * <ul>
 *   <li>DESCRIPTION: {@code serverNowEpochMs}</li>
 *   <li>READY: {@code serverNowEpochMs, playStartEpochMs}</li>
 *   <li>PLAYING: {@code currentNumber, stood, serverNowEpochMs, idleDeadlineEpochMs, hardCapEpochMs}</li>
 *   <li>COLLISION_COOLDOWN: {@code number, collided, serverNowEpochMs, resumeAtEpochMs}</li>
 *   <li>DONE: {@code state}만</li>
 * </ul>
 */
@JsonInclude(Include.NON_NULL)
public record NunchiStateResponse(
        NunchiState state,
        @Nullable Integer currentNumber,
        @Nullable List<String> stood,
        @Nullable Integer number,
        @Nullable List<String> collided,
        @Nullable Long serverNowEpochMs,
        @Nullable Long idleDeadlineEpochMs,
        @Nullable Long hardCapEpochMs,
        @Nullable Long resumeAtEpochMs,
        @Nullable Long playStartEpochMs) {

    public static NunchiStateResponse description(long serverNowEpochMs) {
        return new NunchiStateResponse(
                NunchiState.DESCRIPTION, null, null, null, null, serverNowEpochMs, null, null, null, null);
    }

    public static NunchiStateResponse ready(long serverNowEpochMs, long playStartEpochMs) {
        return new NunchiStateResponse(
                NunchiState.READY, null, null, null, null, serverNowEpochMs, null, null, null, playStartEpochMs);
    }

    public static NunchiStateResponse playing(
            int currentNumber,
            List<String> stood,
            long serverNowEpochMs,
            long idleDeadlineEpochMs,
            long hardCapEpochMs) {
        return new NunchiStateResponse(
                NunchiState.PLAYING,
                currentNumber,
                stood,
                null,
                null,
                serverNowEpochMs,
                idleDeadlineEpochMs,
                hardCapEpochMs,
                null,
                null);
    }

    public static NunchiStateResponse collisionCooldown(
            int number, List<String> collided, long serverNowEpochMs, long resumeAtEpochMs) {
        return new NunchiStateResponse(
                NunchiState.COLLISION_COOLDOWN,
                null,
                null,
                number,
                collided,
                serverNowEpochMs,
                null,
                null,
                resumeAtEpochMs,
                null);
    }

    public static NunchiStateResponse done() {
        return new NunchiStateResponse(NunchiState.DONE, null, null, null, null, null, null, null, null, null);
    }
}
