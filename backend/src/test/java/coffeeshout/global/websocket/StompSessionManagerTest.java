package coffeeshout.global.websocket;

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
        String playerKey = sessionManager.createPlayerKey(joinCode, playerName);

        // then
        assertThat(playerKey).isEqualTo("ABC23:player1");
    }

    @Test
    void joinCode가_null인_경우_예외_발생() {
        // when & then
        assertThatThrownBy(() -> sessionManager.createPlayerKey(null, "player1"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void playerName이_null인_경우_예외_발생() {
        // when & then
        assertThatThrownBy(() -> sessionManager.createPlayerKey("ABC23", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void joinCode에_구분자가_포함된_경우_예외_발생() {
        // when & then
        assertThatThrownBy(() -> sessionManager.createPlayerKey("ABC:23", "player1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("joinCode와 playerName에 구분자(':')가 포함될 수 없습니다");
    }

    @Test
    void playerName에_구분자가_포함된_경우_예외_발생() {
        // when & then
        assertThatThrownBy(() -> sessionManager.createPlayerKey("ABC23", "play:er1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("joinCode와 playerName에 구분자(':')가 포함될 수 없습니다");
    }

    @Test
    void 정상적인_joinCode_추출() {
        // given
        String playerKey = "ABC23:player1";

        // when
        String joinCode = sessionManager.extractJoinCode(playerKey);

        // then
        assertThat(joinCode).isEqualTo("ABC23");
    }

    @Test
    void 정상적인_playerName_추출() {
        // given
        String playerKey = "ABC23:player1";

        // when
        String playerName = sessionManager.extractPlayerName(playerKey);

        // then
        assertThat(playerName).isEqualTo("player1");
    }

    @Test
    void null_플레이어_키로_joinCode_추출_시_예외_발생() {
        // when & then
        assertThatThrownBy(() -> sessionManager.extractJoinCode(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void 구분자가_없는_플레이어_키로_joinCode_추출_시_예외_발생() {
        // when & then
        assertThatThrownBy(() -> sessionManager.extractJoinCode("ABC23player1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("플레이어 키에 구분자(':')가 없습니다: ABC23player1");
    }

    @Test
    void 잘못된_형식의_플레이어_키로_playerName_추출_시_예외_발생() {
        // when & then
        assertThatThrownBy(() -> sessionManager.extractPlayerName("ABC23:player1:extra"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("플레이어 키 형식이 잘못되었습니다. 예상: joinCode:playerName, 실제: ABC23:player1:extra");
    }

    @Test
    void 빈_joinCode가_있는_플레이어_키_검증_시_예외_발생() {
        // when & then
        assertThatThrownBy(() -> sessionManager.extractJoinCode(":player1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("joinCode 또는 playerName이 비어있습니다: :player1");
    }

    @Test
    void 플레이어_키_유효성_검증_정상() {
        // given
        String validPlayerKey = "ABC23:player1";

        // when & then
        assertThat(sessionManager.isValidPlayerKey(validPlayerKey)).isTrue();
    }

    @Test
    void 플레이어_키_유효성_검증_null() {
        // when & then
        assertThat(sessionManager.isValidPlayerKey(null)).isFalse();
    }

    @Test
    void 플레이어_키_유효성_검증_구분자_없음() {
        // when & then
        assertThat(sessionManager.isValidPlayerKey("ABC23player1")).isFalse();
    }

    @Test
    void 플레이어_키_유효성_검증_빈_joinCode() {
        // when & then
        assertThat(sessionManager.isValidPlayerKey(":player1")).isFalse();
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
