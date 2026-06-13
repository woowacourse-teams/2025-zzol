package coffeeshout.gamecommon;

import java.util.Objects;
import lombok.Getter;

/**
 * 모듈 경계를 넘는 게임 참가자 식별자 (ADR-0025).
 *
 * <p>식별은 {@code name}과 {@code userId}로만 한다. {@code colorIndex}는 게임 화면
 * 렌더링용 표시 상태이지 식별자가 아니므로 {@link #equals}/{@link #hashCode}에서 제외한다.
 * 동일 식별의 Gamer가 색상 유무와 무관하게 score 맵 키로 일관되게 매칭되도록 보장한다.
 */
@Getter
public final class Gamer {

    private final String name;
    private final Long userId;
    private final Integer colorIndex;

    public Gamer(String name, Long userId, Integer colorIndex) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.userId = userId;
        this.colorIndex = colorIndex;
    }

    public static Gamer guest(String name) {
        return new Gamer(name, null, null);
    }

    public static Gamer loggedIn(String name, Long userId) {
        return new Gamer(name, userId, null);
    }

    public static Gamer of(String name, Long userId) {
        return new Gamer(name, userId, null);
    }

    public static Gamer of(String name, Long userId, Integer colorIndex) {
        return new Gamer(name, userId, colorIndex);
    }

    public boolean isLoggedIn() {
        return userId != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Gamer gamer)) {
            return false;
        }
        return Objects.equals(name, gamer.name) && Objects.equals(userId, gamer.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, userId);
    }

    @Override
    public String toString() {
        return "Gamer{name='" + name + "', userId=" + userId + ", colorIndex=" + colorIndex + '}';
    }
}
