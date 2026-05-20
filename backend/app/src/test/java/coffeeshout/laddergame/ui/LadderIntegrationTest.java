package coffeeshout.laddergame.ui;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.GameSessionFixture;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import coffeeshout.MessageResponse;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 타이밍 설정 (application-test.yml): description=500ms, prepare=500ms, drawing=1000ms, result=500ms
 */
class LadderIntegrationTest extends WebSocketIntegrationTestSupport {

    JoinCode joinCode;
    Player host;
    TestStompSession session;

    @BeforeEach
    void setUp(
            @Autowired RoomRepository roomRepository,
            @Autowired RoomJpaRepository roomJpaRepository,
            @Autowired JoinCodeGenerator joinCodeGenerator,
            @Autowired GameSessionRepository gameSessionRepository
    ) throws Exception {
        joinCode = joinCodeGenerator.generate();
        Room room = RoomFixture.호스트_꾹이(joinCode);
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        host = room.getHost();

        gameSessionRepository.save(GameSessionFixture.게임세션_게임대기(joinCode, new LadderGame(), host.getName()));

        roomRepository.save(room);
        roomJpaRepository.save(new RoomEntity(joinCode.getValue()));

        session = createSession(joinCode, host.getName());
    }

    @Nested
    class 상태_전환_테스트 {

        @Test
        void 게임_페이즈가_DESCRIPTION_PREPARE_DRAWING_RESULT_DONE_순서로_전환된다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());

            session.send(startCommandUrl(), hostStartCommand());

            final MessageResponse description = stateResponses.get();
            final MessageResponse prepare = stateResponses.get(2, TimeUnit.SECONDS);
            final MessageResponse drawing = stateResponses.get(2, TimeUnit.SECONDS);
            final MessageResponse result = stateResponses.get(3, TimeUnit.SECONDS);
            final MessageResponse done = stateResponses.get(2, TimeUnit.SECONDS);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(description.payload()).contains("\"state\":\"DESCRIPTION\"");
                softly.assertThat(prepare.payload()).contains("\"state\":\"PREPARE\"");
                softly.assertThat(drawing.payload()).contains("\"state\":\"DRAWING\"");
                softly.assertThat(result.payload()).contains("\"state\":\"RESULT\"");
                softly.assertThat(done.payload()).contains("\"state\":\"DONE\"");
            });
        }

        @Test
        void PREPARE_응답에_poles와_bottomRanks가_포함된다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // DESCRIPTION
            final MessageResponse prepare = stateResponses.get(2, TimeUnit.SECONDS);

            assertMessageContains(prepare, "\"state\":\"PREPARE\"");
            assertThat(prepare.payload())
                    .contains("\"poles\":")
                    .contains("\"bottomRanks\":");
        }

        @Test
        void DRAWING_응답에_endTimeEpochMs가_포함된다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            final MessageResponse drawing = stateResponses.get(2, TimeUnit.SECONDS);

            assertMessageContains(drawing, "\"state\":\"DRAWING\"");
            assertThat(drawing.payload()).contains("\"endTimeEpochMs\":");
        }

        @Test
        void RESULT_응답에_rankings와_animationDurationMs가_포함된다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING
            final MessageResponse result = stateResponses.get(3, TimeUnit.SECONDS);

            assertMessageContains(result, "\"state\":\"RESULT\"");
            assertThat(result.payload())
                    .contains("\"rankings\":")
                    .contains("\"animationDurationMs\":500");
        }

        @Test
        void DONE_응답에는_state_필드만_포함된다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING
            stateResponses.get(3, TimeUnit.SECONDS); // RESULT
            final MessageResponse done = stateResponses.get(2, TimeUnit.SECONDS);

            assertMessageContains(done, "\"state\":\"DONE\"");
            assertThat(done.payload())
                    .doesNotContain("\"poles\":")
                    .doesNotContain("\"rankings\":");
        }
    }

    @Nested
    class 선_긋기_테스트 {

        @Test
        void DRAWING_중_선_긋기_요청이_line_토픽으로_브로드캐스트된다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());
            final var lineResponses = session.subscribe(lineUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

            session.send(drawCommandUrl(), drawRequest(0));

            final MessageResponse lineResponse = lineResponses.get();

            assertMessageContains(lineResponse, "\"playerName\":\"꾹이\"");
            assertMessageContains(lineResponse, "\"segmentIndex\":0");
            assertThat(lineResponse.payload()).contains("\"row\":");
        }

        @Test
        void 브로드캐스트된_선의_row는_양수다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());
            final var lineResponses = session.subscribe(lineUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

            session.send(drawCommandUrl(), drawRequest(0));

            final MessageResponse lineResponse = lineResponses.get();

            // "row": 뒤에 1 이상의 숫자가 오는지 검증
            assertThat(lineResponse.payload()).containsPattern("\"row\":\\s*[1-9]\\d*");
        }

        @Test
        void 이미_선을_그은_플레이어의_재요청은_브로드캐스트되지_않는다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());
            final var lineResponses = session.subscribe(lineUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

            session.send(drawCommandUrl(), drawRequest(0)); // 첫 번째 요청
            lineResponses.get(); // 브로드캐스트 수신

            session.send(drawCommandUrl(), drawRequest(1)); // 중복 요청

            lineResponses.assertNoMessage();
        }

        @Test
        void 유효하지_않은_segmentIndex_요청은_브로드캐스트되지_않는다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());
            final var lineResponses = session.subscribe(lineUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

            // 4명 플레이어 → 유효 구간 0~2, 3은 유효하지 않음
            session.send(drawCommandUrl(), drawRequest(3));

            lineResponses.assertNoMessage();
        }

        @Test
        void 여러_플레이어가_각자_선을_그을_수_있다() throws Exception {
            final TestStompSession 루키세션 = createSession(joinCode.getValue(), "루키");

            final var stateResponses = session.subscribe(stateUrl());
            final var lineResponses = session.subscribe(lineUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

            session.send(drawCommandUrl(), drawRequest(0));
            lineResponses.get(); // 꾹이 선 브로드캐스트

            루키세션.send(drawCommandUrl(), drawRequest(2));
            final MessageResponse 루키라인 = lineResponses.get();

            assertMessageContains(루키라인, "\"playerName\":\"루키\"");
            assertMessageContains(루키라인, "\"segmentIndex\":2");
        }
    }

    // ---- 헬퍼 메서드 ----

    private String stateUrl() {
        return String.format("/topic/room/%s/ladder/state", joinCode.getValue());
    }

    private String lineUrl() {
        return String.format("/topic/room/%s/ladder/line", joinCode.getValue());
    }

    private String startCommandUrl() {
        return String.format("/app/room/%s/minigame/command", joinCode.getValue());
    }

    private String drawCommandUrl() {
        return String.format("/app/room/%s/ladder/draw", joinCode.getValue());
    }

    private String hostStartCommand() {
        return String.format("""
                {
                  "commandType": "START_MINI_GAME",
                  "commandRequest": { "hostName": "%s" }
                }
                """, host.getName().value());
    }

    private String drawRequest(int segmentIndex) {
        return String.format("""
                {
                  "segmentIndex": %d
                }
                """, segmentIndex);
    }
}
