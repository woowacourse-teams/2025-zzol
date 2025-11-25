package coffeeshout.room.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.service.RoomCommandService;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class DelayedRoomRemovalServiceTest {

    @Mock
    RoomCommandService roomCommandService;

    @Mock
    TaskScheduler taskScheduler;

    @SuppressWarnings({"rawtypes"})
    ScheduledFuture scheduledFuture = mock(ScheduledFuture.class);

    DelayedRoomRemovalService delayedRoomRemovalService;

    JoinCode joinCode;

    @BeforeEach
    void setUp() {
        Duration removalDelay = Duration.ofMillis(100);

        delayedRoomRemovalService = new DelayedRoomRemovalService(
                taskScheduler,
                removalDelay,
                roomCommandService
        );

        joinCode = new JoinCode("ABCD");
    }

    @Nested
    class 방_지연_삭제_스케줄링 {

        @Test
        @SuppressWarnings("unchecked")
        void 정상적으로_지연_삭제를_스케줄링한다() {
            given(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .willReturn(scheduledFuture);

            delayedRoomRemovalService.scheduleRemoveRoom(joinCode);

            then(taskScheduler).should().schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void 서로_다른_방은_독립적으로_스케줄링된다() {
            JoinCode joinCode1 = new JoinCode("ABCD");
            JoinCode joinCode2 = new JoinCode("FGHK");
            given(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .willReturn(scheduledFuture);

            delayedRoomRemovalService.scheduleRemoveRoom(joinCode1);
            delayedRoomRemovalService.scheduleRemoveRoom(joinCode2);

            then(taskScheduler).should(times(2)).schedule(any(Runnable.class), any(Instant.class));
        }
    }

    @Nested
    class 실제_삭제_실행_시뮬레이션 {

        @Test
        @SuppressWarnings("unchecked")
        void RoomCommandService가_정상_호출된다() {
            given(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .willAnswer(invocation -> {
                        Runnable task = invocation.getArgument(0);
                        task.run();
                        return scheduledFuture;
                    });

            delayedRoomRemovalService.scheduleRemoveRoom(joinCode);

            then(roomCommandService).should().delete(joinCode);
        }

        @Test
        @SuppressWarnings("unchecked")
        void RoomCommandService에서_예외_발생해도_안전하게_처리한다() {
            willThrow(new RuntimeException("방 삭제 실패"))
                    .given(roomCommandService)
                    .delete(any(JoinCode.class));

            given(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .willAnswer(invocation -> {
                        Runnable task = invocation.getArgument(0);
                        task.run();
                        return scheduledFuture;
                    });

            delayedRoomRemovalService.scheduleRemoveRoom(joinCode);

            then(roomCommandService).should().delete(joinCode);
        }
    }
}
