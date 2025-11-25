package coffeeshout.global.websocket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SubscriptionInfoService {

    // destination별 구독자 수를 직접 관리
    private final ConcurrentMap<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

    // subscriptionId -> destination 매핑 관리
    private final ConcurrentMap<String, String> subscriptionToDestination = new ConcurrentHashMap<>();

    /**
     * 구독 추가
     */
    public void addSubscription(String sessionId, String destination, String subscriptionId) {
        subscriptions.computeIfAbsent(destination, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        subscriptionToDestination.put(subscriptionId, destination);
        log.debug("구독 추가: sessionId={}, destination={}, subscriptionId={}, 현재 구독자수={}",
                sessionId, destination, subscriptionId, getSubscriberCount(destination));
    }

    /**
     * 구독 제거
     */
    public void removeSubscription(String sessionId, String destination) {
        Set<String> sessions = subscriptions.get(destination);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                subscriptions.remove(destination);
            }
        }
        log.debug("구독 제거: sessionId={}, destination={}, 현재 구독자수={}",
                sessionId, destination, getSubscriberCount(destination));
    }

//    /**
//     * subscriptionId로 구독 제거
//     */
//    public void removeSubscriptionById(String sessionId, String subscriptionId) {
//        String destination = subscriptionToDestination.remove(subscriptionId);
//        if (destination != null) {
//            removeSubscription(sessionId, destination);
//        } else {
//            log.warn("subscriptionId에 해당하는 destination을 찾을 수 없음: subscriptionId={}", subscriptionId);
//        }
//    }

    /**
     * 세션의 모든 구독 제거
     */
    public void removeAllSubscriptions(String sessionId) {
        // subscriptionId -> destination 매핑에서 해당 세션 관련 항목들 제거
        subscriptionToDestination.entrySet().removeIf(entry -> {
            String destination = entry.getValue();
            Set<String> sessions = subscriptions.get(destination);
            return sessions != null && sessions.contains(sessionId);
        });

        // destination별 구독에서 세션 제거
        subscriptions.entrySet().removeIf(entry -> {
            entry.getValue().remove(sessionId);
            return entry.getValue().isEmpty();
        });
        log.debug("세션의 모든 구독 제거: sessionId={}", sessionId);
    }

    /**
     * 특정 destination의 구독자 수 조회
     */
    public int getSubscriberCount(String destination) {
        Set<String> sessions = subscriptions.get(destination);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * 특정 destination의 구독 정보 로깅
     */
    public void logSubscriptionInfo(String destination) {
        int count = getSubscriberCount(destination);

        if (count == 0) {
            log.info("구독자 없음: destination={}", destination);
            return;
        }

        log.info("구독 정보: destination={}, 구독자 수={}", destination, count);

        Set<String> sessions = subscriptions.get(destination);
        if (sessions != null) {
            sessions.forEach(sessionId ->
                    log.info("  - sessionId={}", sessionId)
            );
        }
    }

    /**
     * 전체 구독 정보 조회 (디버깅용)
     */
    public void logAllSubscriptions() {
        log.info("=== 전체 구독 정보 ===");
        subscriptions.forEach((destination, sessions) ->
                log.info("destination={}, 구독자수={}", destination, sessions.size())
        );
    }
}
