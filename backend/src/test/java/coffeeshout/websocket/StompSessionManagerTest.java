package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThatThrownBy(() -> PlayerKey.of("ABC:23", "player1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("구분자");
    }

    @Test
    void playerName에_구분자가_포함된_경우_예외_발생() {
        // when & then
        assertThatThrownBy(() -> PlayerKey.of("ABC23", "play:er1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("구분자");
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
        assertThatThrownBy(() -> PlayerKey.parse("ABC23player1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("형식이 잘못되었습니다");
    }

    @Test
    void 잘못된_형식의_플레이어_키로_parse_시_예외_발생() {
        // when & then
        assertThatThrownBy(() -> PlayerKey.parse("ABC23:player1:extra"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("형식이 잘못되었습니다");
    }

    @Test
    void 빈_joinCode가_있는_플레이어_키_parse_시_예외_발생() {
        // when & then
        assertThatThrownBy(() -> PlayerKey.parse(":player1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("형식이 잘못되었습니다");
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
}
