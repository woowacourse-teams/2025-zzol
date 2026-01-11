package coffeeshout.dashboard.infra.persistence;

import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.global.config.QueryDslConfig;
import coffeeshout.minigame.domain.MiniGameType;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL 환경에서 Dashboard 쿼리 성능 테스트
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, QueryDslDashboardStatisticsRepository.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryPerformanceTest {

    private static final int MYSQL_PORT = 3306;

    static GenericContainer<?> mysql = new GenericContainer<>(DockerImageName.parse("mysql:8.0"))
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            .withEnv("MYSQL_DATABASE", "coffeeshout_test")
            .withEnv("MYSQL_USER", "test")
            .withEnv("MYSQL_PASSWORD", "test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
            .waitingFor(Wait.forListeningPort());

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = "jdbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(MYSQL_PORT) + "/coffeeshout_test"
                + "?rewriteBatchedStatements=true";  // Batch INSERT 성능 최적화
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private DashboardStatisticsRepository dashboardStatisticsRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private javax.sql.DataSource dataSource;

    private static final int TOTAL_ROOMS = 100_000;
    private static final int MIN_PLAYERS_PER_ROOM = 4;
    private static final int MAX_PLAYERS_PER_ROOM = 8;
    private static final int BATCH_SIZE = 10000;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean dataInitialized = false;

    @BeforeEach
    void setUp() {
        YearMonth currentMonth = YearMonth.now();
        startDate = currentMonth.atDay(1).atStartOfDay();
        endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        ensureTestDataExists();
    }

    /**
     * 더미데이터가 없으면 생성, 있으면 스킵
     */
    private void ensureTestDataExists() {
        if (dataInitialized) {
            return;
        }

        Long count = em.createQuery("SELECT COUNT(r) FROM RouletteResultEntity r", Long.class)
                .getSingleResult();

        if (count == 0) {
            System.out.println("=== 더미데이터 생성 ===");
            long dataStart = System.currentTimeMillis();
            insertBulkTestData();
            System.out.println("생성 완료: " + (System.currentTimeMillis() - dataStart) + "ms\n");
        } else {
            System.out.println("=== 기존 데이터 사용 (" + count + "건) ===\n");
        }

        dataInitialized = true;
    }

    @Test
    @Commit
    @DisplayName("EXPLAIN ANALYZE - findLowestProbabilityWinner")
    void explain_findLowestProbabilityWinner() {
        String sql = """
                EXPLAIN ANALYZE
                SELECT DISTINCT
                    r.winner_probability,
                    p.player_name
                FROM roulette_result r
                JOIN player p ON r.winner_id = p.id
                WHERE r.created_at BETWEEN :startDate AND :endDate
                  AND r.winner_probability = (
                      SELECT MIN(sub.winner_probability)
                      FROM roulette_result sub
                      WHERE sub.created_at BETWEEN :startDate AND :endDate
                  )
                ORDER BY p.player_name ASC
                LIMIT 5
                """;

        @SuppressWarnings("unchecked")
        List<String> explainResult = em.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

        System.out.println("=== EXPLAIN ANALYZE 결과 ===");
        explainResult.forEach(System.out::println);
    }

    @Test
    @Commit
    @DisplayName("findLowestProbabilityWinner 성능 측정")
    void measure_findLowestProbabilityWinner() {
        // 웜업
        System.out.println("=== 웜업 (3회) ===");
        for (int i = 0; i < 3; i++) {
            dashboardStatisticsRepository.findLowestProbabilityWinner(startDate, endDate, 5);
        }
        em.clear();

        // 측정
        System.out.println("=== 성능 측정 (10회) ===");
        int iterations = 10;
        long totalTime = 0;
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            em.clear();
            long start = System.currentTimeMillis();
            dashboardStatisticsRepository.findLowestProbabilityWinner(startDate, endDate, 5);
            long elapsed = System.currentTimeMillis() - start;
            times.add(elapsed);
            totalTime += elapsed;
            System.out.println((i + 1) + "회: " + elapsed + "ms");
        }

        System.out.println("\n=== 결과 ===");
        System.out.println("평균: " + (totalTime / iterations) + "ms");
        System.out.println("최소: " + times.stream().mapToLong(Long::longValue).min().orElse(0) + "ms");
        System.out.println("최대: " + times.stream().mapToLong(Long::longValue).max().orElse(0) + "ms");
    }

    @Test
    @Commit
    @DisplayName("findTopWinnersBetween 성능 측정")
    void measure_findTopWinnersBetween() {
        // 웜업
        for (int i = 0; i < 3; i++) {
            dashboardStatisticsRepository.findTopWinnersBetween(startDate, endDate, 5);
        }
        em.clear();

        System.out.println("=== 성능 측정 (10회) ===");
        long totalTime = 0;
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            em.clear();
            long start = System.currentTimeMillis();
            dashboardStatisticsRepository.findTopWinnersBetween(startDate, endDate, 5);
            long elapsed = System.currentTimeMillis() - start;
            times.add(elapsed);
            totalTime += elapsed;
            System.out.println((i + 1) + "회: " + elapsed + "ms");
        }

        System.out.println("\n=== 결과 ===");
        System.out.println("평균: " + (totalTime / 10) + "ms");
        System.out.println("최소: " + times.stream().mapToLong(Long::longValue).min().orElse(0) + "ms");
        System.out.println("최대: " + times.stream().mapToLong(Long::longValue).max().orElse(0) + "ms");
    }

    @Test
    @Commit
    @DisplayName("findGamePlayCountByMonth 성능 측정")
    void measure_findGamePlayCountByMonth() {
        // 웜업
        for (int i = 0; i < 3; i++) {
            dashboardStatisticsRepository.findGamePlayCountByMonth(startDate, endDate);
        }
        em.clear();

        System.out.println("=== 성능 측정 (10회) ===");
        long totalTime = 0;
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            em.clear();
            long start = System.currentTimeMillis();
            dashboardStatisticsRepository.findGamePlayCountByMonth(startDate, endDate);
            long elapsed = System.currentTimeMillis() - start;
            times.add(elapsed);
            totalTime += elapsed;
            System.out.println((i + 1) + "회: " + elapsed + "ms");
        }

        System.out.println("\n=== 결과 ===");
        System.out.println("평균: " + (totalTime / 10) + "ms");
        System.out.println("최소: " + times.stream().mapToLong(Long::longValue).min().orElse(0) + "ms");
        System.out.println("최대: " + times.stream().mapToLong(Long::longValue).max().orElse(0) + "ms");
    }

    @Test
    @Commit
    @DisplayName("findRacingGameTopPlayers 성능 측정")
    void measure_findRacingGameTopPlayers() {
        // 웜업
        System.out.println("=== 웜업 (3회) ===");
        for (int i = 0; i < 3; i++) {
            dashboardStatisticsRepository.findRacingGameTopPlayers(startDate, endDate, 5);
        }
        em.clear();

        System.out.println("=== 성능 측정 (10회) ===");
        long totalTime = 0;
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            em.clear();
            long start = System.currentTimeMillis();
            dashboardStatisticsRepository.findRacingGameTopPlayers(startDate, endDate, 5);
            long elapsed = System.currentTimeMillis() - start;
            times.add(elapsed);
            totalTime += elapsed;
            System.out.println((i + 1) + "회: " + elapsed + "ms");
        }

        System.out.println("\n=== 결과 ===");
        System.out.println("평균: " + (totalTime / 10) + "ms");
        System.out.println("최소: " + times.stream().mapToLong(Long::longValue).min().orElse(0) + "ms");
        System.out.println("최대: " + times.stream().mapToLong(Long::longValue).max().orElse(0) + "ms");
    }

    @Test
    @Commit
    @DisplayName("EXPLAIN ANALYZE - findRacingGameTopPlayers")
    void explain_findRacingGameTopPlayers() {
        String sql = """
                EXPLAIN ANALYZE
                SELECT
                    p.player_name,
                    AVG(mr.player_rank) as avg_rank,
                    SUM(mr.score) as total_score
                FROM mini_game_result mr
                JOIN player p ON mr.player_id = p.id
                WHERE mr.mini_game_type = 'RACING_GAME'
                  AND mr.created_at BETWEEN :startDate AND :endDate
                GROUP BY p.player_name
                ORDER BY avg_rank ASC
                LIMIT 5
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> explainResult = em.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

        System.out.println("=== EXPLAIN ANALYZE 결과 ===");
        for (Object row : explainResult) {
            System.out.println(row);
        }
    }

    private void insertBulkTestData() {
        Random random = new Random(42);
        LocalDateTime baseDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        int totalDays = 375;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // PreparedStatement 준비
            try (PreparedStatement roomStmt = conn.prepareStatement(
                    "INSERT INTO room_session (join_code, room_status, created_at, finished_at) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement playerStmt = conn.prepareStatement(
                         "INSERT INTO player (room_session_id, player_name, player_type, created_at) VALUES (?, ?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement miniGameStmt = conn.prepareStatement(
                         "INSERT INTO mini_game_play (room_session_id, mini_game_type) VALUES (?, ?)",
                         Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement miniGameResultStmt = conn.prepareStatement(
                         "INSERT INTO mini_game_result (mini_game_play_id, player_id, player_rank, score, mini_game_type, created_at) VALUES (?, ?, ?, ?, ?, ?)");
                 PreparedStatement rouletteStmt = conn.prepareStatement(
                         "INSERT INTO roulette_result (room_session_id, winner_id, winner_probability, created_at) VALUES (?, ?, ?, ?)")) {

                // 1. Room 데이터 준비 및 삽입
                List<RoomData> roomDataList = prepareRoomData(random, baseDate, totalDays);
                long baseRoomId = insertRooms(conn, roomStmt, roomDataList, random);

                // 2. Player 삽입
                int totalPlayers = insertPlayers(playerStmt, roomDataList, baseRoomId);

                // 3. MiniGame 삽입
                int totalMiniGames = insertMiniGames(miniGameStmt, roomDataList, baseRoomId);

                // 4. Results 삽입
                long basePlayerId = getMinId(conn, "player");
                long baseMiniGameId = getMinId(conn, "mini_game_play");
                int totalMiniGameResults = insertMiniGameResults(miniGameResultStmt, roomDataList, basePlayerId, baseMiniGameId, random);
                insertRouletteResults(rouletteStmt, roomDataList, baseRoomId, basePlayerId, random);

                conn.commit();

                printStatistics(totalPlayers, totalMiniGames, totalMiniGameResults);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to insert bulk test data", e);
        }
    }

    private List<RoomData> prepareRoomData(Random random, LocalDateTime baseDate, int totalDays) {
        System.out.println("  RoomData 준비 중...");
        List<RoomData> roomDataList = new ArrayList<>(TOTAL_ROOMS);

        for (int roomIdx = 0; roomIdx < TOTAL_ROOMS; roomIdx++) {
            int dayOffset = (roomIdx * totalDays) / TOTAL_ROOMS;
            LocalDateTime roomCreatedAt = baseDate.plusDays(dayOffset)
                    .plusHours(random.nextInt(24))
                    .plusMinutes(random.nextInt(60));

            int playerCount = MIN_PLAYERS_PER_ROOM + random.nextInt(MAX_PLAYERS_PER_ROOM - MIN_PLAYERS_PER_ROOM + 1);
            int miniGameCount = 1 + random.nextInt(3);

            List<MiniGameType> miniGameTypes = new ArrayList<>();
            for (int i = 0; i < miniGameCount; i++) {
                miniGameTypes.add(MiniGameType.values()[random.nextInt(MiniGameType.values().length)]);
            }

            roomDataList.add(new RoomData(roomCreatedAt, playerCount, miniGameTypes));
        }

        return roomDataList;
    }

    private long insertRooms(Connection conn, PreparedStatement roomStmt, List<RoomData> roomDataList, Random random) throws Exception {
        System.out.println("  Room 생성 중...");

        for (int roomIdx = 0; roomIdx < TOTAL_ROOMS; roomIdx++) {
            RoomData roomData = roomDataList.get(roomIdx);
            String joinCode = generateJoinCode(roomIdx);
            Timestamp timestamp = Timestamp.valueOf(roomData.createdAt);

            roomStmt.setString(1, joinCode);
            roomStmt.setString(2, "FINISHED");
            roomStmt.setTimestamp(3, timestamp);
            roomStmt.setTimestamp(4, timestamp);
            roomStmt.addBatch();

            if ((roomIdx + 1) % BATCH_SIZE == 0) {
                roomStmt.executeBatch();
                System.out.println("    " + (roomIdx + 1) + " rooms inserted");
            }
        }
        roomStmt.executeBatch();

        return getMinId(conn, "room_session");
    }

    private int insertPlayers(PreparedStatement playerStmt, List<RoomData> roomDataList, long baseRoomId) throws Exception {
        System.out.println("  Player 생성 중...");
        long currentRoomId = baseRoomId;
        int playerBatchCount = 0;
        int totalPlayers = 0;

        for (int roomIdx = 0; roomIdx < TOTAL_ROOMS; roomIdx++) {
            RoomData roomData = roomDataList.get(roomIdx);
            long roomId = currentRoomId++;
            Timestamp timestamp = Timestamp.valueOf(roomData.createdAt);

            for (int playerIdx = 0; playerIdx < roomData.playerCount; playerIdx++) {
                String playerType = playerIdx == 0 ? "HOST" : "GUEST";
                playerStmt.setLong(1, roomId);
                playerStmt.setString(2, "P" + roomIdx + "_" + playerIdx);
                playerStmt.setString(3, playerType);
                playerStmt.setTimestamp(4, timestamp);
                playerStmt.addBatch();
                totalPlayers++;

                if (++playerBatchCount >= BATCH_SIZE) {
                    playerStmt.executeBatch();
                    playerBatchCount = 0;
                }
            }

            if ((roomIdx + 1) % 10000 == 0) {
                System.out.println("    " + (roomIdx + 1) + "개 방의 플레이어 추가 완료");
            }
        }
        playerStmt.executeBatch();

        return totalPlayers;
    }

    private int insertMiniGames(PreparedStatement miniGameStmt, List<RoomData> roomDataList, long baseRoomId) throws Exception {
        System.out.println("  MiniGame 생성 중...");
        long currentRoomId = baseRoomId;
        int miniGameBatchCount = 0;
        int totalMiniGames = 0;

        for (int roomIdx = 0; roomIdx < TOTAL_ROOMS; roomIdx++) {
            RoomData roomData = roomDataList.get(roomIdx);
            long roomId = currentRoomId++;

            for (MiniGameType gameType : roomData.miniGameTypes) {
                miniGameStmt.setLong(1, roomId);
                miniGameStmt.setString(2, gameType.name());
                miniGameStmt.addBatch();
                totalMiniGames++;

                if (++miniGameBatchCount >= BATCH_SIZE) {
                    miniGameStmt.executeBatch();
                    miniGameBatchCount = 0;
                }
            }
        }
        miniGameStmt.executeBatch();

        return totalMiniGames;
    }

    private int insertMiniGameResults(PreparedStatement miniGameResultStmt, List<RoomData> roomDataList,
                                      long basePlayerId, long baseMiniGameId, Random random) throws Exception {
        System.out.println("  MiniGameResult 생성 중...");
        long currentPlayerId = basePlayerId;
        long currentMiniGameId = baseMiniGameId;
        int miniGameResultBatchCount = 0;
        int totalMiniGameResults = 0;

        for (int roomIdx = 0; roomIdx < TOTAL_ROOMS; roomIdx++) {
            RoomData roomData = roomDataList.get(roomIdx);
            Timestamp timestamp = Timestamp.valueOf(roomData.createdAt);

            // 이 방의 플레이어 ID 목록
            List<Long> playerIds = new ArrayList<>();
            for (int playerIdx = 0; playerIdx < roomData.playerCount; playerIdx++) {
                playerIds.add(currentPlayerId++);
            }

            // 이 방의 미니게임별 결과 생성
            for (MiniGameType gameType : roomData.miniGameTypes) {
                long miniGameId = currentMiniGameId++;

                List<Long> shuffledPlayerIds = new ArrayList<>(playerIds);
                java.util.Collections.shuffle(shuffledPlayerIds, random);

                for (int rank = 1; rank <= shuffledPlayerIds.size(); rank++) {
                    long playerId = shuffledPlayerIds.get(rank - 1);
                    long score = (shuffledPlayerIds.size() + 1 - rank) * 1000L + random.nextInt(500);

                    miniGameResultStmt.setLong(1, miniGameId);
                    miniGameResultStmt.setLong(2, playerId);
                    miniGameResultStmt.setInt(3, rank);
                    miniGameResultStmt.setLong(4, score);
                    miniGameResultStmt.setString(5, gameType.name());
                    miniGameResultStmt.setTimestamp(6, timestamp);
                    miniGameResultStmt.addBatch();
                    totalMiniGameResults++;

                    if (++miniGameResultBatchCount >= BATCH_SIZE) {
                        miniGameResultStmt.executeBatch();
                        miniGameResultBatchCount = 0;
                    }
                }
            }

            if ((roomIdx + 1) % 10000 == 0) {
                System.out.println("    " + (roomIdx + 1) + "개 방 결과 추가 완료");
            }
        }
        miniGameResultStmt.executeBatch();

        return totalMiniGameResults;
    }

    private void insertRouletteResults(PreparedStatement rouletteStmt, List<RoomData> roomDataList,
                                       long baseRoomId, long basePlayerId, Random random) throws Exception {
        System.out.println("  RouletteResult 생성 중...");
        long currentRoomId = baseRoomId;
        long currentPlayerId = basePlayerId;
        int rouletteBatchCount = 0;

        for (int roomIdx = 0; roomIdx < TOTAL_ROOMS; roomIdx++) {
            RoomData roomData = roomDataList.get(roomIdx);
            long roomId = currentRoomId++;
            Timestamp timestamp = Timestamp.valueOf(roomData.createdAt);

            // 이 방의 플레이어 중 랜덤으로 승자 선택
            List<Long> playerIds = new ArrayList<>();
            for (int playerIdx = 0; playerIdx < roomData.playerCount; playerIdx++) {
                playerIds.add(currentPlayerId++);
            }

            long winnerId = playerIds.get(random.nextInt(playerIds.size()));
            int probability = Math.max(1, Math.min(100, (int) (20 + random.nextGaussian() * 10)));

            rouletteStmt.setLong(1, roomId);
            rouletteStmt.setLong(2, winnerId);
            rouletteStmt.setInt(3, probability);
            rouletteStmt.setTimestamp(4, timestamp);
            rouletteStmt.addBatch();

            if (++rouletteBatchCount >= BATCH_SIZE) {
                rouletteStmt.executeBatch();
                rouletteBatchCount = 0;
            }
        }
        rouletteStmt.executeBatch();
    }

    private long getMinId(Connection conn, String tableName) throws Exception {
        ResultSet rs = conn.createStatement().executeQuery("SELECT MIN(id) FROM " + tableName);
        rs.next();
        return rs.getLong(1);
    }

    private void printStatistics(int totalPlayers, int totalMiniGames, int totalMiniGameResults) {
        System.out.println("=== 생성 완료 ===");
        System.out.println("방: " + TOTAL_ROOMS + "개");
        System.out.println("플레이어: " + totalPlayers + "명 (방당 평균 " + (totalPlayers / TOTAL_ROOMS) + "명)");
        System.out.println("미니게임: " + totalMiniGames + "개 (방당 평균 " + (totalMiniGames / TOTAL_ROOMS) + "개)");
        System.out.println("미니게임 결과: " + totalMiniGameResults + "건");
        System.out.println("룰렛 결과: " + TOTAL_ROOMS + "건");
        System.out.println("날짜 범위: 2025-01-01 ~ 2026-01-10 (약 1년치)");
    }

    // 데이터 구조 클래스
    private static class RoomData {
        LocalDateTime createdAt;
        int playerCount;
        List<MiniGameType> miniGameTypes;

        RoomData(LocalDateTime createdAt, int playerCount, List<MiniGameType> miniGameTypes) {
            this.createdAt = createdAt;
            this.playerCount = playerCount;
            this.miniGameTypes = miniGameTypes;
        }
    }


    private String generateJoinCode(int index) {
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        int num = index;
        for (int i = 0; i < 5; i++) {
            sb.insert(0, base.charAt(num % 26));
            num /= 26;
        }
        return sb.toString();
    }
}
