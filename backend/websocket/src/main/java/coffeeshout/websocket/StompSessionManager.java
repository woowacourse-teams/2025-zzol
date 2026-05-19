package coffeeshout.websocket;

import static org.springframework.util.Assert.isTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StompSessionManager {

    // 중복 처리 방지용
    private final Set<String> processedDisconnections = ConcurrentHashMap.newKeySet();

    // 플레이어 세션 매핑 관리
    private final ConcurrentHashMap<String, String> playerSessionMap; // "joinCode:playerName" -> sessionId
    private final ConcurrentHashMap<String, String> sessionPlayerMap; // sessionId -> "joinCode:playerName"

    public StompSessionManager() {
        this.playerSessionMap = new ConcurrentHashMap<>();
        this.sessionPlayerMap = new ConcurrentHashMap<>();
    }

    /**
     * 플레이어 세션 매핑 등록
     */
    public void registerPlayerSession(@NonNull String joinCode, @NonNull String playerName, @NonNull String sessionId) {
        final String playerKey = PlayerKey.of(joinCode, playerName).toString();
        upsertSessionMapping(playerKey, sessionId);
    }

    public void registerPlayerSession(@NonNull String playerKey, @NonNull String sessionId) {
        if (!PlayerKey.isValid(playerKey)) {
            throw new IllegalArgumentException("잘못된 플레이어 키 형식: " + playerKey);
        }
        upsertSessionMapping(playerKey, sessionId);
    }

    private void upsertSessionMapping(@NonNull String playerKey, @NonNull String sessionId) {
        // 기존 세션이 있으면 정리
        final String oldSessionId = playerSessionMap.get(playerKey);
        if (oldSessionId != null) {
            log.info("기존 플레이어 세션 정리: playerKey={}, oldSessionId={}", playerKey, oldSessionId);
            sessionPlayerMap.remove(oldSessionId);
        }

        playerSessionMap.put(playerKey, sessionId);
        sessionPlayerMap.put(sessionId, playerKey);
        log.info("플레이어 세션 매핑 등록: playerKey={}, sessionId={}", playerKey, sessionId);
    }

    /**
     * 플레이어의 기존 세션 ID 조회
     */
    public boolean hasSessionId(@NonNull String joinCode, @NonNull String playerName) {
        final String playerKey = PlayerKey.of(joinCode, playerName).toString();
        return playerSessionMap.containsKey(playerKey);
    }

    /**
     * 플레이어 키 존재 여부 확인 (Internal)
     */
    public boolean hasPlayerKeyInternal(@NonNull String playerKey) {
        return playerSessionMap.containsKey(playerKey);
    }

    public String getSessionId(@NonNull String joinCode, @NonNull String playerName) {
        final String playerKey = PlayerKey.of(joinCode, playerName).toString();

        isTrue(playerSessionMap.containsKey(playerKey),
                "플레이어 세션이 존재하지 않습니다: joinCode=%s, playerName=%s".formatted(joinCode, playerName));

        return playerSessionMap.get(playerKey);
    }

    /**
     * 세션 ID로 플레이어 키 조회
     */
    public boolean hasPlayerKey(@NonNull String sessionId) {
        return sessionPlayerMap.containsKey(sessionId);
    }

    public String getPlayerKey(@NonNull String sessionId) {
        isTrue(sessionPlayerMap.containsKey(sessionId),
                "세션 ID가 존재하지 않습니다: sessionId=%s".formatted(sessionId));

        return sessionPlayerMap.get(sessionId);
    }

    /**
     * 세션 매핑 제거 (Internal - Redis 이벤트 핸들러용)
     */
    public void removeSession(@NonNull String sessionId) {
        final String playerKey = sessionPlayerMap.remove(sessionId);
        if (playerKey != null) {
            playerSessionMap.remove(playerKey);
            log.info("세션 매핑 제거: playerKey={}, sessionId={}", playerKey, sessionId);
        }

        // 중복 disconnect 방지 세트 정리(메모리 누수 방지 목적)
        processedDisconnections.remove(sessionId);
    }

    /**
     * 중복 disconnection 처리 방지를 위한 체크 및 등록
     */
    public boolean isDisconnectionProcessed(@NonNull String sessionId) {
        return !processedDisconnections.add(sessionId);
    }

    /**
     * 특정 방의 연결된 플레이어 수 조회
     */
    public long getConnectedPlayerCountByJoinCode(@NonNull String joinCode) {
        final String prefix = PlayerKey.prefix(joinCode);
        return playerSessionMap.keySet().stream()
                .filter(playerKey -> playerKey.startsWith(prefix))
                .count();
    }

    /**
     * 현재 인스턴스에 연결된 전체 클라이언트 수 조회
     */
    public int getTotalConnectedClientCount() {
        return sessionPlayerMap.size();
    }
}
