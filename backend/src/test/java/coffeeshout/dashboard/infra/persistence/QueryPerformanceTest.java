package coffeeshout.dashboard.infra.persistence;

import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.global.config.QueryDslConfig;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameResultEntity;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RouletteResultEntity;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.util.ReflectionTestUtils;
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
        String jdbcUrl = "jdbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(MYSQL_PORT) + "/coffeeshout_test";
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private DashboardStatisticsRepository dashboardStatisticsRepository;

    @Autowired
    private EntityManager em;

    private static final int TOTAL_ROOMS = 100_000;
    private static final int MIN_PLAYERS_PER_ROOM = 4;
    private static final int MAX_PLAYERS_PER_ROOM = 8;

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
        
        // 1년치 데이터 분산 (2025-01-01 ~ 2025-12-31 + 2026-01-01 ~ 현재)
        LocalDateTime baseDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        int totalDays = 375; // 약 1년 + 10일

        int totalPlayers = 0;
        int totalMiniGames = 0;
        int totalMiniGameResults = 0;

        for (int roomIdx = 0; roomIdx < TOTAL_ROOMS; roomIdx++) {
            // 날짜 분산: roomIdx를 기반으로 날짜 계산
            int dayOffset = (roomIdx * totalDays) / TOTAL_ROOMS;
            LocalDateTime roomCreatedAt = baseDate.plusDays(dayOffset)
                    .plusHours(random.nextInt(24))
                    .plusMinutes(random.nextInt(60));

            RoomEntity room = createRoom(generateJoinCode(roomIdx), roomCreatedAt);
            em.persist(room);

            // 방당 4~8명의 플레이어 (랜덤)
            int playerCount = MIN_PLAYERS_PER_ROOM + random.nextInt(MAX_PLAYERS_PER_ROOM - MIN_PLAYERS_PER_ROOM + 1);
            List<PlayerEntity> roomPlayers = new ArrayList<>();
            for (int playerIdx = 0; playerIdx < playerCount; playerIdx++) {
                PlayerType type = playerIdx == 0 ? PlayerType.HOST : PlayerType.GUEST;
                PlayerEntity player = createPlayer(room, "P" + roomIdx + "_" + playerIdx, type, roomCreatedAt);
                em.persist(player);
                roomPlayers.add(player);
            }
            totalPlayers += playerCount;

            // 방당 미니게임 1~3개 (랜덤, 최소 1개 보장)
            int miniGameCount = 1 + random.nextInt(3);
            for (int gameIdx = 0; gameIdx < miniGameCount; gameIdx++) {
                MiniGameType gameType = MiniGameType.values()[random.nextInt(MiniGameType.values().length)];
                MiniGameEntity miniGame = new MiniGameEntity(room, gameType);
                em.persist(miniGame);
                totalMiniGames++;

                // 미니게임 결과 (플레이어별 순위, 점수)
                List<PlayerEntity> shuffledPlayers = new ArrayList<>(roomPlayers);
                java.util.Collections.shuffle(shuffledPlayers, random);
                for (int rank = 1; rank <= shuffledPlayers.size(); rank++) {
                    PlayerEntity player = shuffledPlayers.get(rank - 1);
                    long score = (shuffledPlayers.size() + 1 - rank) * 1000L + random.nextInt(500);
                    MiniGameResultEntity miniGameResult = createMiniGameResult(miniGame, player, rank, score, roomCreatedAt);
                    em.persist(miniGameResult);
                    totalMiniGameResults++;
                }
            }

            // 방당 룰렛 결과 1개
            PlayerEntity winner = roomPlayers.get(random.nextInt(roomPlayers.size()));
            int probability = Math.max(1, Math.min(100, (int) (20 + random.nextGaussian() * 10)));
            RouletteResultEntity rouletteResult = createRouletteResult(room, winner, probability, roomCreatedAt);
            em.persist(rouletteResult);

            if (roomIdx % 1000 == 0 && roomIdx > 0) {
                em.flush();
                em.clear();
                System.out.println("  " + roomIdx + "개 방 생성...");
            }
        }
        em.flush();
        em.clear();
        System.out.println("=== 생성 완료 ===");
        System.out.println("방: " + TOTAL_ROOMS + "개");
        System.out.println("플레이어: " + totalPlayers + "명 (방당 평균 " + (totalPlayers / TOTAL_ROOMS) + "명)");
        System.out.println("미니게임: " + totalMiniGames + "개 (방당 평균 " + (totalMiniGames / TOTAL_ROOMS) + "개)");
        System.out.println("미니게임 결과: " + totalMiniGameResults + "건");
        System.out.println("룰렛 결과: " + TOTAL_ROOMS + "건");
        System.out.println("날짜 범위: 2025-01-01 ~ 2026-01-10 (약 1년치)");
    }

    private RoomEntity createRoom(String joinCode, LocalDateTime createdAt) {
        RoomEntity room = new RoomEntity(joinCode);
        ReflectionTestUtils.setField(room, "createdAt", createdAt);
        return room;
    }

    private PlayerEntity createPlayer(RoomEntity room, String playerName, PlayerType playerType, LocalDateTime createdAt) {
        PlayerEntity player = new PlayerEntity(room, playerName, playerType);
        ReflectionTestUtils.setField(player, "createdAt", createdAt);
        return player;
    }

    private RouletteResultEntity createRouletteResult(RoomEntity room, PlayerEntity winner, int probability, LocalDateTime createdAt) {
        RouletteResultEntity result = new RouletteResultEntity(room, winner, probability);
        ReflectionTestUtils.setField(result, "createdAt", createdAt);
        return result;
    }

    private MiniGameResultEntity createMiniGameResult(MiniGameEntity miniGame, PlayerEntity player, int rank, long score, LocalDateTime createdAt) {
        MiniGameResultEntity result = new MiniGameResultEntity(miniGame, player, rank, score);
        ReflectionTestUtils.setField(result, "createdAt", createdAt);
        ReflectionTestUtils.setField(result, "miniGameType", miniGame.getMiniGameType());
        return result;
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
