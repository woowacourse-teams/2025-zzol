package coffeeshout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.config.IntegrationTestConfig;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.room.infra.websocket.StompPrincipalInterceptor;
import coffeeshout.settlement.infra.SettlementStreamKey;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.WebSocketIntegrationTestSupport;
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompHeaders;

@SpringBootTest(classes = GameModuleTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(IntegrationTestConfig.class)
public abstract class GameModuleWebSocketTest extends WebSocketIntegrationTestSupport {

    private static final String SMOKE_JOIN_CODE = "SMOK";
    private static final String SMOKE_PLAYER_NAME = "smoketest";

    @Autowired
    private RoomSessionTokenService roomSessionTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    /**
     * 4인 명단(꾹이·루키·엠제이·한스) 중 루키를 회원으로 둔다. 정산 Outbox는 {@code userId != null}인
     * 플레이어만 담으므로 전원 게스트면 정산 이벤트 자체가 만들어지지 않는다. 호스트를 회원으로 두면 세션
     * 호스트(게스트로 생성) 동일성 비교가 얽히므로 루키다. user_code는 UNIQUE이고 DB 정리 실패는 로그만
     * 남기고 삼켜지므로 joinCode로 테스트마다 다른 코드를 쓴다.
     */
    protected List<Gamer> 루키가_회원인_명단(JoinCode joinCode) {
        final Long userId = userJpaRepository
                .save(new UserEntity("T" + joinCode.getValue(), "루키"))
                .getId();
        return GamerFixture.꾹이_루키_엠제이_한스().stream()
                .map(gamer ->
                        "루키".equals(gamer.getName()) ? Gamer.of(gamer.getName(), userId, gamer.getColorIndex()) : gamer)
                .toList();
    }

    /**
     * 미니게임 종료 → 결과 저장 이음매를 검증한다(#1663). DONE 브로드캐스트는 결과 저장보다 앞줄에 예약되므로
     * 종료 신호만 보는 테스트는 저장이 통째로 깨져도 초록으로 남는다(#1662). 저장은 스케줄러 스레드에서
     * 일어나므로 폴링으로 기다린다.
     *
     * <p>정산 이벤트는 결과 저장과 같은 트랜잭션에서 Outbox에 기록되고(#1610), 릴레이는 행을 지우지 않고
     * 상태만 바꾼다. payload의 게임 타입까지 거는 이유는 앞선 테스트가 남긴 종료 태스크가 이 테스트 도중
     * 발화할 수 있어서다. {@code LIKE}에서 {@code _}는 와일드카드라 이스케이프한다.
     */
    protected void 결과_저장과_정산_아웃박스를_확인한다(MiniGameType miniGameType, int playerCount) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(저장된_결과_수(miniGameType)).isEqualTo(playerCount);
            assertThat(정산_아웃박스_수(miniGameType)).isEqualTo(1);
        });
    }

    private Integer 저장된_결과_수(MiniGameType miniGameType) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mini_game_result WHERE mini_game_type = ?", Integer.class, miniGameType.name());
    }

    private Integer 정산_아웃박스_수(MiniGameType miniGameType) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE stream_key = ? AND payload LIKE ? ESCAPE '!'",
                Integer.class,
                SettlementStreamKey.RESULT.getRedisKey(),
                "%" + miniGameType.name().replace("_", "!_") + "%");
    }

    protected TestStompSession createSession() throws InterruptedException, ExecutionException, TimeoutException {
        final String token = roomSessionTokenService.issue(SMOKE_JOIN_CODE, SMOKE_PLAYER_NAME, null);
        return createSessionWithRoomToken(token);
    }

    protected TestStompSession createSession(JoinCode joinCode, PlayerName playerName)
            throws InterruptedException, ExecutionException, TimeoutException {
        return createSession(joinCode.getValue(), playerName.value());
    }

    protected TestStompSession createSession(String joinCode, String playerName)
            throws InterruptedException, ExecutionException, TimeoutException {
        final String token = roomSessionTokenService.issue(joinCode, playerName, null);
        return createSessionWithRoomToken(token);
    }

    protected TestStompSession createSessionWithRoomToken(String roomToken)
            throws InterruptedException, ExecutionException, TimeoutException {
        final StompHeaders headers = new StompHeaders();
        headers.add(StompPrincipalInterceptor.ROOM_TOKEN_HEADER, roomToken);
        return connect(headers);
    }
}
