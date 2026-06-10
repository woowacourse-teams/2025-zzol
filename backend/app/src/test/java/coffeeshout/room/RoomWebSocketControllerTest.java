package coffeeshout.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import coffeeshout.gamecommon.GameRoomCreatedEvent;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.room.ui.request.MiniGameSelectMessage;
import coffeeshout.room.ui.request.ReadyChangeMessage;
import coffeeshout.room.ui.request.RouletteSpinMessage;
import coffeeshout.room.ui.response.PlayerResponse;
import coffeeshout.room.ui.response.WinnerResponse;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.app.WebSocketIntegrationTestSupport;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
               @Autowired PlatformTransactionManager transactionManager,
               @Autowired JoinCodeGenerator joinCodeGenerator,
               @Autowired StreamPublisher streamPublisher,
               @Autowired GameSessionRepository gameSessionRepository
    ) throws Exception {
        joinCode = joinCodeGenerator.generate();
        testRoom = RoomFixture.호스트_꾹이(joinCode);
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

        // 게임 선택 흐름은 GameSession 사전 생성을 전제하므로(ADR-0023 결정 4), 실제 방 생성과 동일하게
        // GameRoomCreatedEvent를 스트림에 발행해 GameSessionInitConsumer가 권위 있는 호스트로 세션을 만들도록 한다.
        streamPublisher.publish(RoomStreamKey.BROADCAST,
                new GameRoomCreatedEvent(host.getName().value(), joinCode.getValue()));

        // 비동기 컨슈머의 세션 생성 완료를 보장한 뒤 진행한다.
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> gameSessionRepository.existsByJoinCode(joinCode));

        session = createSession(joinCode, host.getName());
    }

    @Test
    void 플레이어_목록을_조회한다() {
        String subscribeUrl = String.format("/topic/room/%s", joinCode.getValue());
        String requestUrl = String.format("/app/room/%s/update-players", joinCode.getValue());

        var responses = session.subscribe(subscribeUrl);

        session.send(requestUrl);

        List<PlayerResponse> players = payloadAsList(responses.get(), PlayerResponse.class);

        assertThat(players)
                .extracting(PlayerResponse::playerName, PlayerResponse::playerType,
                        PlayerResponse::isReady, PlayerResponse::probability)
                .containsExactly(
                        tuple("꾹이", PlayerType.HOST, true, 25.0),
                        tuple("루키", PlayerType.GUEST, false, 25.0),
                        tuple("엠제이", PlayerType.GUEST, false, 25.0),
                        tuple("한스", PlayerType.GUEST, false, 25.0)
                );
    }

    @Test
    void 준비_상태를_변경한다() {
        String subscribeUrl = String.format("/topic/room/%s", joinCode.getValue());
        String requestUrl = String.format("/app/room/%s/update-ready", joinCode.getValue());

        var responses = session.subscribe(subscribeUrl);

        session.send(requestUrl, new ReadyChangeMessage(joinCode.getValue(), "루키", false));

        List<PlayerResponse> players = payloadAsList(responses.get(), PlayerResponse.class);

        assertThat(players)
                .extracting(PlayerResponse::playerName, PlayerResponse::playerType,
                        PlayerResponse::isReady, PlayerResponse::probability)
                .containsExactly(
                        tuple("꾹이", PlayerType.HOST, true, 25.0),
                        tuple("루키", PlayerType.GUEST, false, 25.0),
                        tuple("엠제이", PlayerType.GUEST, false, 25.0),
                        tuple("한스", PlayerType.GUEST, false, 25.0)
                );
    }

    @Test
    void 미니게임을_선택한다() {
        String subscribeUrl = String.format("/topic/room/%s/minigame", joinCode.getValue());
        String requestUrl = String.format("/app/room/%s/update-minigames", joinCode.getValue());

        var responses = session.subscribe(subscribeUrl);

        session.send(requestUrl, new MiniGameSelectMessage(host.getName().value(), List.of(MiniGameType.CARD_GAME)));

        List<MiniGameType> miniGameTypes = payloadAsList(responses.get(), MiniGameType.class);

        assertThat(miniGameTypes).containsExactly(MiniGameType.CARD_GAME);
    }

    @Test
    void 룰렛을_돌려서_당첨자를_선택한다() {
        ReflectionTestUtils.setField(testRoom, "roomState", RoomState.ROULETTE);

        String subscribeUrl = String.format("/topic/room/%s/winner", joinCode.getValue());
        String requestUrl = String.format("/app/room/%s/spin-roulette", joinCode.getValue());

        var responses = session.subscribe(subscribeUrl);

        session.send(requestUrl, new RouletteSpinMessage(host.getName().value()));

        // 룰렛 결과는 랜덤이므로 당첨자가 방의 플레이어 중 하나인지만 검증한다
        WinnerResponse winner = payloadAs(responses.get(), WinnerResponse.class);
        assertThat(winner.playerName()).isIn("꾹이", "루키", "엠제이", "한스");
    }
}
