package coffeeshout.room.ui;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import coffeeshout.global.MessageResponse;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class RoomWebSocketControllerTest extends WebSocketIntegrationTestSupport {

    JoinCode joinCode;

    Player host;

    TestStompSession session;

    Room testRoom;

    @BeforeEach
    void setUp(@Autowired RoomRepository roomRepository,
               @Autowired RoomJpaRepository roomJpaRepository,
               @Autowired PlayerJpaRepository playerJpaRepository,
               @Autowired PlatformTransactionManager transactionManager) throws Exception {
        testRoom = RoomFixture.호스트_꾹이();
        joinCode = testRoom.getJoinCode();  // Room에서 실제 joinCode 가져오기
        host = testRoom.getHost();

        // MemoryRepository에 저장
        roomRepository.save(testRoom);

        // 새로운 독립 트랜잭션으로 DB에 저장 (비동기 이벤트 핸들러가 조회할 수 있도록)
        var txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        txTemplate.executeWithoutResult(status -> {
            // DB에 RoomEntity 저장
            RoomEntity roomEntity = new RoomEntity(joinCode.getValue());
            roomJpaRepository.save(roomEntity);

            // DB에 PlayerEntity들 저장 (룰렛 결과 저장 시 필요)
            testRoom.getPlayers().forEach(player -> {
                PlayerEntity playerEntity = new PlayerEntity(
                        roomEntity,
                        player.getName().value(),
                        player.getPlayerType()
                );
                playerJpaRepository.save(playerEntity);
            });
        });

        session = createSession();
    }

    @Test
    void 플레이어_목록을_조회한다() throws JSONException {
        // given
        String joinCodeValue = joinCode.getValue();
        String subscribeUrlFormat = String.format("/topic/room/%s", joinCodeValue);
        String requestUrlFormat = String.format("/app/room/%s/update-players", joinCodeValue);

        var responses = session.subscribe(subscribeUrlFormat);

        // when
        session.send(requestUrlFormat);

        // then
        MessageResponse playersResponse = responses.get();
        assertMessage(playersResponse, """
                {
                   "success":true,
                   "data":[
                      {
                         "playerName":"꾹이",
                         "playerType":"HOST",
                         "isReady":true,
                         "probability": 25.0
                      },
                      {
                         "playerName":"루키",
                         "playerType":"GUEST",
                         "isReady":false,
                         "probability": 25.0
                      },
                      {
                         "playerName":"엠제이",
                         "playerType":"GUEST",
                         "isReady":false,
                         "probability": 25.0
                      },
                      {
                         "playerName":"한스",
                         "playerType":"GUEST",
                         "isReady":false,
                         "probability": 25.0
                      }
                   ]
                }
                """);
    }

    @Test
    void 준비_상태를_변경한다() throws JSONException {
        // given
        String subscribeUrlFormat = String.format("/topic/room/%s", joinCode.getValue());
        String requestUrlFormat = String.format("/app/room/%s/update-ready", joinCode.getValue());

        var responses = session.subscribe(subscribeUrlFormat);

        // when
        session.send(requestUrlFormat, String.format("""
                {
                  "joinCode": "%s",
                  "playerName": "루키",
                  "isReady": false
                }
                """, joinCode.getValue()));

        // then
        MessageResponse readyResponse = responses.get();

        assertMessage(readyResponse, """
                {
                   "success":true,
                   "data":[
                      {
                         "playerName":"꾹이",
                         "playerType":"HOST",
                         "isReady":true,
                         "probability": 25.0
                      },
                      {
                         "playerName":"루키",
                         "playerType":"GUEST",
                         "isReady":false,
                         "probability": 25.0
                      },
                      {
                         "playerName":"엠제이",
                         "playerType":"GUEST",
                         "isReady":false,
                         "probability": 25.0
                      },
                      {
                         "playerName":"한스",
                         "playerType":"GUEST",
                         "isReady":false,
                         "probability": 25.0
                      }
                   ]
                }
                """);
    }

    @Test
    void 미니게임을_선택한다() throws JSONException {
        // given
        String subscribeUrlFormat = String.format("/topic/room/%s/minigame", joinCode.getValue());
        String requestUrlFormat = String.format("/app/room/%s/update-minigames", joinCode.getValue());

        var responses = session.subscribe(subscribeUrlFormat);

        // when
        session.send(requestUrlFormat, String.format("""
                {
                  "hostName": "%s",
                  "miniGameTypes": ["CARD_GAME"]
                }
                """, host.getName().value()));

        // then
        MessageResponse miniGameResponse = responses.get();

        assertMessage(miniGameResponse, """
                {
                   "success":true,
                   "data":["CARD_GAME"]
                }
                """);
    }

    @Test
    void 룰렛을_돌려서_당첨자를_선택한다() {
        // given
        ReflectionTestUtils.setField(testRoom, "roomState", RoomState.ROULETTE);

        String subscribeUrlFormat = String.format("/topic/room/%s/winner", joinCode.getValue());
        String requestUrlFormat = String.format("/app/room/%s/spin-roulette", joinCode.getValue());

        var responses = session.subscribe(subscribeUrlFormat);

        // when
        session.send(requestUrlFormat, String.format("""
                {
                  "hostName": "%s"
                }
                """, host.getName().value()));

        // then
        MessageResponse winnerResponse = responses.get();

        // 룰렛 결과는 랜덤이므로 assertMessageContains 사용
        assertMessageContains(winnerResponse, "\"success\":true");
        assertMessageContains(winnerResponse, "\"playerName\":");
    }

    private static Customization getColorIndexCustomization(String path) {
        return new Customization(path, (actual, expect) -> {
            if (expect instanceof Integer value) {
                return value >= 0 && value <= 9;
            }
            return true;
        });
    }
}
