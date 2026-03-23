package coffeeshout.global.websocket;

import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.room.application.service.RoomService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * DelayedPlayerRemovalService의 실제 타이밍 기반 통합 테스트 실제 TaskScheduler를 사용하여 지연 실행을 테스트한다.
 */
@ExtendWith(MockitoExtension.class)
class DelayedPlayerRemovalServiceIntegrationTest {

    private static final Duration TEST_REMOVAL_DELAY = Duration.ofMillis(500);

    @Mock
    private PlayerDisconnectionService playerDisconnectionService;

    @Mock
    private RoomService roomService;

    private ThreadPoolTaskScheduler taskScheduler;
    private DelayedPlayerRemovalService delayedPlayerRemovalService;
    private StompSessionManager stompSessionManager;

    private final String playerKey = "ABC23:김철수";
    private final String sessionId = "session-123";
    private final String reason = "CLIENT_DISCONNECT";

    @BeforeEach
    void setUp() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        taskScheduler.setThreadNamePrefix("test-scheduler-");
        taskScheduler.initialize();
        stompSessionManager = new StompSessionManager();

        delayedPlayerRemovalService = new DelayedPlayerRemovalService(taskScheduler, TEST_REMOVAL_DELAY,
                playerDisconnectionService, stompSessionManager, roomService);
    }

    @Nested
    class 실제_지연_실행_테스트 {

        @Test
        void 지연시간_후_실제로_PlayerDisconnectionService가_호출된다() {
            // given
            given(roomService.isReadyState("ABC23")).willReturn(true);

            // when
            delayedPlayerRemovalService.schedulePlayerRemoval(playerKey, sessionId, reason);

            // then
            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> {
                        then(playerDisconnectionService).should()
                                .handlePlayerDisconnection(playerKey, sessionId, reason);
                    });
        }

        @Test
        void 지연시간_내에_취소하면_실행되지_않는다() {
            // given
            given(roomService.isReadyState("ABC23")).willReturn(true);
            delayedPlayerRemovalService.schedulePlayerRemoval(playerKey, sessionId, reason);

            // when - 즉시 취소
            delayedPlayerRemovalService.cancelScheduledRemoval(playerKey);

            // then - 지연시간이 지나도 실행되지 않음
            await().during(Duration.ofMillis(700))
                    .atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        then(playerDisconnectionService).should(never())
                                .handlePlayerDisconnection(playerKey, sessionId, reason);
                    });
        }
    }

    @Nested
    class 동시성_실제_테스트 {

        @Test
        void 여러_플레이어가_동시에_스케줄링되어도_안전하게_처리된다() {
            // given
            String player1 = "ABC23:김철수";
            String player2 = "DEF56:박영희";
            String player3 = "GHI89:이민수";

            given(roomService.isReadyState("ABC23")).willReturn(true);
            given(roomService.isReadyState("DEF56")).willReturn(true);
            given(roomService.isReadyState("GHI89")).willReturn(true);

            // when - 동시에 여러 플레이어 스케줄링
            delayedPlayerRemovalService.schedulePlayerRemoval(player1, "session-1", reason);
            delayedPlayerRemovalService.schedulePlayerRemoval(player2, "session-2", reason);
            delayedPlayerRemovalService.schedulePlayerRemoval(player3, "session-3", reason);

            // then - 모든 플레이어가 독립적으로 처리됨
            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> {
                        then(playerDisconnectionService).should()
                                .handlePlayerDisconnection(player1, "session-1", reason);
                        then(playerDisconnectionService).should()
                                .handlePlayerDisconnection(player2, "session-2", reason);
                        then(playerDisconnectionService).should()
                                .handlePlayerDisconnection(player3, "session-3", reason);
                    });
        }
    }

    @Nested
    class 리소스_정리_테스트 {

        @Test
        void 실행_완료된_태스크는_맵에서_자동_제거된다() {
            // given
            given(roomService.isReadyState("ABC23")).willReturn(true);

            // when
            delayedPlayerRemovalService.schedulePlayerRemoval(playerKey, sessionId, reason);

            // then - 실행 완료 후 PlayerDisconnectionService 호출됨을 확인
            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> {
                        then(playerDisconnectionService).should()
                                .handlePlayerDisconnection(playerKey, sessionId, reason);
                    });
        }

        @Test
        void 취소된_태스크는_PlayerDisconnectionService를_호출하지_않는다() {
            // given
            given(roomService.isReadyState("ABC23")).willReturn(true);
            delayedPlayerRemovalService.schedulePlayerRemoval(playerKey, sessionId, reason);

            // when
            delayedPlayerRemovalService.cancelScheduledRemoval(playerKey);

            // then - 취소 후 일정 시간이 지나도 호출되지 않음
            await().during(Duration.ofMillis(700))
                    .atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        then(playerDisconnectionService).should(never())
                                .handlePlayerDisconnection(playerKey, sessionId, reason);
                    });
        }
    }
}
