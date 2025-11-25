package coffeeshout.room.infra.messaging;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoomEventWaitManager {

    private final ConcurrentHashMap<String, CompletableFuture<?>> pendingEvents = new ConcurrentHashMap<>();

    public <T> CompletableFuture<T> registerWait(String eventId) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        pendingEvents.putIfAbsent(eventId, future);

        log.debug("방 이벤트 Future 등록: eventId={}", eventId);

        // 자동 정리: Future 완료 시 pendingEvents에서 제거
        future.whenComplete((result, throwable) -> {
            pendingEvents.remove(eventId);
            if (throwable != null) {
                log.error("방 이벤트 자동 정리 (실패): eventId={}", eventId, throwable);
                return;
            }
            log.debug("방 이벤트 자동 정리 (성공): eventId={}", eventId);
        });

        return future;
    }

    @SuppressWarnings("unchecked")
    public <T> void notifySuccess(String eventId, T result) {
        final CompletableFuture<T> future = (CompletableFuture<T>) pendingEvents.get(eventId);

        if (future == null) {
            log.warn("방 이벤트 성공 알림 실패: eventId={}에 대한 Future가 존재하지 않습니다.", eventId);
            return;
        }

        future.complete(result);
        log.debug("방 이벤트 성공 알림: eventId={}", eventId);
    }

    public void notifyFailure(String eventId, Throwable throwable) {
        final CompletableFuture<?> future = pendingEvents.get(eventId);

        if (future == null) {
            log.warn("방 이벤트 실패 알림 시도: eventId={}에 해당하는 Future가 존재하지 않습니다.", eventId);
            return;
        }

        future.completeExceptionally(throwable);
        log.debug("방 이벤트 실패 알림: eventId={}", eventId);
    }


    /**
     * 예외적 상황에서만 사용하는 수동 정리 메서드 (예: 강제 취소, 시스템 종료 등) 일반적인 경우는 registerWait의 자동 정리를 사용
     */
    public void forceCleanup(String eventId) {
        CompletableFuture<?> future = pendingEvents.remove(eventId);
        if (future != null) {
            future.cancel(true);
            log.warn("방 이벤트 강제 정리: eventId={}", eventId);
        }
    }
}
