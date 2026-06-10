package coffeeshout.fixture;

import coffeeshout.gamecommon.Gamer;
import java.util.List;

/**
 * {@code RoomFixture.호스트_꾹이}와 동일한 4인 명단(꾹이·루키·엠제이·한스)을 Room 의존 없이 제공한다.
 * colorIndex를 명시해 프로덕션 gamer(방 입장 시 색 부여)와 동일하게 화면 렌더링용 표시 상태를 채운다.
 */
public final class GamerFixture {

    private GamerFixture() {
    }

    public static Gamer 호스트_꾹이() {
        return Gamer.of("꾹이", null, 0);
    }

    public static List<Gamer> 꾹이_루키_엠제이_한스() {
        return List.of(
                호스트_꾹이(),
                Gamer.of("루키", null, 1),
                Gamer.of("엠제이", null, 2),
                Gamer.of("한스", null, 3)
        );
    }
}
