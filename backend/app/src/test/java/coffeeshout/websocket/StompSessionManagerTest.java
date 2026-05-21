package coffeeshout.websocket;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.websocket.PlayerKeyErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StompSessionManagerTest {

    StompSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new StompSessionManager();
    }

    @Test
    void 정상적인_플레이어_키_생성() {
        // given
        String joinCode = "ABC23";
        String playerName = "player1";

        // when
        String playerKey = PlayerKey.of(joinCode, playerName).toString();

        // then
        assertThat(playerKey).isEqualTo("ABC23:player1");
    }

    @Test
    void joinCode가_null인_경우_예외_발생() {
        // when & then
        assertThatThrownBy(() -> PlayerKey.of(null, "player1"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void playerName이_null인_경우_예외_발생() {
        // when & then
        assertThatThrownBy(() -> PlayerKey.of("ABC23", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void joinCode에_구분자가_포함된_경우_예외_발생() {
        // when & then
        assertCoffeeShoutException(
                () -> PlayerKey.of("ABC:23", "player1"),
                PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT
        );
    }

    @Test
    void playerName에_구분자가_포함된_경우_예외_발생() {
        // when & then
        assertCoffeeShoutException(
                () -> PlayerKey.of("ABC23", "play:er1"),
                PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT
        );
    }

    @Test
    void 정상적인_joinCode_추출() {
        // given
        String playerKeyStr = "ABC23:player1";

        // when
        PlayerKey playerKey = PlayerKey.parse(playerKeyStr);

        // then
        assertThat(playerKey.joinCode()).isEqualTo("ABC23");
    }

    @Test
    void 정상적인_playerName_추출() {
        // given
        String playerKeyStr = "ABC23:player1";

        // when
        PlayerKey playerKey = PlayerKey.parse(playerKeyStr);

        // then
        assertThat(playerKey.playerName()).isEqualTo("player1");
    }

    @Test
    void null_플레이어_키로_parse_시_예외_발생() {
        // when & then
        assertThatThrownBy(() -> PlayerKey.parse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void 구분자가_없는_플레이어_키로_parse_시_예외_발생() {
        // when & then
        assertCoffeeShoutException(
                () -> PlayerKey.parse("ABC23player1"),
                PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT
        );
    }

    @Test
    void 잘못된_형식의_플레이어_키로_parse_시_예외_발생() {
        // when & then
        assertCoffeeShoutException(
                () -> PlayerKey.parse("ABC23:player1:extra"),
                PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT
        );
    }

    @Test
    void 빈_joinCode가_있는_플레이어_키_parse_시_예외_발생() {
        // when & then
        assertCoffeeShoutException(
                () -> PlayerKey.parse(":player1"),
                PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT
        );
    }

    @Test
    void 플레이어_키_유효성_검증_정상() {
        // given
        String validPlayerKey = "ABC23:player1";

        // when & then
        assertThat(PlayerKey.isValid(validPlayerKey)).isTrue();
    }

    @Test
    void 플레이어_키_유효성_검증_null() {
        // when & then
        assertThat(PlayerKey.isValid(null)).isFalse();
    }

    @Test
    void 플레이어_키_유효성_검증_구분자_없음() {
        // when & then
        assertThat(PlayerKey.isValid("ABC23player1")).isFalse();
    }

    @Test
    void 플레이어_키_유효성_검증_빈_joinCode() {
        // when & then
        assertThat(PlayerKey.isValid(":player1")).isFalse();
    }

    @Test
    void 플레이어_세션_등록_및_조회() {
        // given
        String joinCode = "ABC23";
        String playerName = "player1";
        String sessionId = "session123";

        // when
        sessionManager.registerPlayerSession(joinCode, playerName, sessionId);

        // then
        assertThat(sessionManager.getSessionId(joinCode, playerName)).isEqualTo(sessionId);
        assertThat(sessionManager.getPlayerKey(sessionId)).isEqualTo("ABC23:player1");
    }

    @Test
    void 특정_방의_연결된_플레이어_수_조회() {
        // given
        String joinCode = "ABC23";
        sessionManager.registerPlayerSession(joinCode, "player1", "session1");
        sessionManager.registerPlayerSession(joinCode, "player2", "session2");
        sessionManager.registerPlayerSession("XYZ789", "player3", "session3");

        // when
        long count = sessionManager.getConnectedPlayerCountByJoinCode(joinCode);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void 같은_닉네임이라도_userId가_다르면_별도의_세션으로_관리된다() {
        // given - 비로그인 게스트와 로그인 사용자가 동일 닉네임으로 각각 WebSocket 연결
        String joinCode = "ABC23";
        String playerName = "꾹이";
        String guestKey = PlayerKey.of(joinCode, playerName).toString();           // "ABC23:꾹이"
        String loggedInKey = PlayerKey.of(joinCode, playerName, 100L).toString(); // "ABC23:꾹이:100"

        // when - SessionConnectEventListener가 parsed PlayerKey를 그대로 사용해 등록
        sessionManager.registerPlayerSession(guestKey, "session-guest");
        sessionManager.registerPlayerSession(loggedInKey, "session-100");

        // then - 두 세션이 독립적으로 유지 (게스트 세션이 덮어써지지 않음)
        assertThat(sessionManager.getPlayerKey("session-guest")).isEqualTo(guestKey);
        assertThat(sessionManager.getPlayerKey("session-100")).isEqualTo(loggedInKey);
        assertThat(sessionManager.getConnectedPlayerCountByJoinCode(joinCode)).isEqualTo(2);
    }

    @Test
    void userId_없이_같은_닉네임으로_등록하면_세션이_덮어써진다() {
        // 이 테스트는 fix 이전의 잘못된 동작을 문서화한다.
        // SessionConnectEventListener가 userId를 버리고 "joinCode:playerName"만으로 등록하면
        // 동일 닉네임의 두 번째 플레이어가 첫 번째 플레이어 세션을 덮어쓴다.
        String joinCode = "ABC23";
        String keyWithoutUserId = PlayerKey.of(joinCode, "꾹이").toString(); // "ABC23:꾹이"

        sessionManager.registerPlayerSession(keyWithoutUserId, "session-guest");
        sessionManager.registerPlayerSession(keyWithoutUserId, "session-100"); // 덮어씀

        // 게스트 세션은 사라지고 session-100만 남는다
        assertThat(sessionManager.getConnectedPlayerCountByJoinCode(joinCode)).isEqualTo(1);
        assertThat(sessionManager.getPlayerKey("session-100")).isEqualTo(keyWithoutUserId);
    }
}
