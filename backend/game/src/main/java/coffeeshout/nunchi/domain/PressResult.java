package coffeeshout.nunchi.domain;

import coffeeshout.gamecommon.Gamer;
import java.util.List;

/**
 * {@link NunchiGame#press} 결과. {@code STOOD}/{@code COLLIDED}이면 그 번호를 알리고,
 * {@code COLLIDED}이면 충돌 그룹 전원을 함께 싣는다(상태 브로드캐스트용).
 */
public record PressResult(PressOutcome outcome, int number, List<Gamer> collidedGroup) {

    public static PressResult stood(int number) {
        return new PressResult(PressOutcome.STOOD, number, List.of());
    }

    public static PressResult collided(int number, List<Gamer> collidedGroup) {
        return new PressResult(PressOutcome.COLLIDED, number, List.copyOf(collidedGroup));
    }

    public static PressResult ignored() {
        return new PressResult(PressOutcome.IGNORED, 0, List.of());
    }
}
