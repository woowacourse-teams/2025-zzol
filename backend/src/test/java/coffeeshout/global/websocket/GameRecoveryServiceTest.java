package coffeeshout.global.websocket;

import static coffeeshout.cardgame.domain.event.CardGameMessagePublisher.CARD_GAME_STATE_DESTINATION_FORMAT;
import static coffeeshout.cardgame.domain.event.CardGameMessagePublisher.GAME_START_DESTINATION_FORMAT;
import static coffeeshout.global.websocket.GameRecoveryService.ID_MAP_KEY_FORMAT;
import static coffeeshout.global.websocket.GameRecoveryService.STREAM_KEY_FORMAT;
import static coffeeshout.racinggame.infra.messaging.RacingGameMessagePublisher.RACING_GAME_PLAYERS_POSITION_DESTINATION_FORMAT;
import static coffeeshout.racinggame.infra.messaging.RacingGameMessagePublisher.RACING_GAME_STATE_DESTINATION_FORMAT;
import static coffeeshout.room.ui.messaging.RoomMessagePublisher.MINI_GAME_TOPIC_FORMAT;
import static coffeeshout.room.ui.messaging.RoomMessagePublisher.PLAYER_LIST_TOPIC_FORMAT;
import static coffeeshout.room.ui.messaging.RoomMessagePublisher.QR_CODE_TOPIC_FORMAT;
import static coffeeshout.room.ui.messaging.RoomMessagePublisher.ROULETTE_TOPIC_FORMAT;
import static coffeeshout.room.ui.messaging.RoomMessagePublisher.WINNER_TOPIC_FORMAT;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.global.ServiceTest;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import coffeeshout.global.websocket.ui.dto.RecoveryMessage;
import coffeeshout.room.domain.JoinCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;

class GameRecoveryServiceTest extends ServiceTest {

    @Autowired
    GameRecoveryService gameRecoveryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;


    private JoinCode joinCode;

    @BeforeEach
    void setUp() {
        joinCode = new JoinCode("ABCD");
        // 테스트 전 Redis 정리
        cleanupRedis(joinCode);
    }

    private void cleanupRedis(JoinCode joinCode) {
        String streamKey = String.format(STREAM_KEY_FORMAT, joinCode);
        String idMapKey = String.format(ID_MAP_KEY_FORMAT, joinCode);
        stringRedisTemplate.delete(List.of(streamKey, idMapKey));
    }

    @Nested
    @DisplayName("메시지 ID 생성 테스트")
    class 메시지_ID_생성 {

        @Test
        @DisplayName("destination과 response 데이터가 같으면 같은 메시지 ID를 반환한다")
        void 같은_데이터면_같은_메시지_ID를_반환한다() {
            // given
            String destination = "/topic/room/ABCD";
            WebSocketResponse<String> response = WebSocketResponse.success("test data");

            // when
            String messageId1 = gameRecoveryService.generateMessageId(destination, response);
            String messageId2 = gameRecoveryService.generateMessageId(destination, response);

            // then
            assertThat(messageId1).isEqualTo(messageId2);
        }

        @Test
        @DisplayName("destination이 다르면 다른 메시지 ID를 반환한다")
        void destination이_다르면_다른_메시지_ID를_반환한다() {
            // given
            String destination1 = "/topic/room/ABCD";
            String destination2 = "/topic/room/EFGH";
            WebSocketResponse<String> response = WebSocketResponse.success("test data");

            // when
            String messageId1 = gameRecoveryService.generateMessageId(destination1, response);
            String messageId2 = gameRecoveryService.generateMessageId(destination2, response);

            // then
            assertThat(messageId1).isNotEqualTo(messageId2);
        }

        @Test
        @DisplayName("response 데이터가 다르면 다른 메시지 ID를 반환한다")
        void response가_다르면_다른_메시지_ID를_반환한다() {
            // given
            String destination = "/topic/room/ABCD";
            WebSocketResponse<String> response1 = WebSocketResponse.success("data1");
            WebSocketResponse<String> response2 = WebSocketResponse.success("data2");

            // when
            String messageId1 = gameRecoveryService.generateMessageId(destination, response1);
            String messageId2 = gameRecoveryService.generateMessageId(destination, response2);

            // then
            assertThat(messageId1).isNotEqualTo(messageId2);
        }

        @ParameterizedTest
        @DisplayName("다양한 destination에 대해 일관된 메시지 ID를 생성한다")
        @ValueSource(strings = {
                "/topic/room/ABCD",
                "/topic/game/start",
                "/queue/user/messages",
                "/topic/room/ABCD/players"
        })
        void 다양한_destination에_대해_일관된_ID를_생성한다(String destination) {
            // given
            WebSocketResponse<String> response = WebSocketResponse.success("test");

            // when
            String messageId1 = gameRecoveryService.generateMessageId(destination, response);
            String messageId2 = gameRecoveryService.generateMessageId(destination, response);

            // then
            assertThat(messageId1)
                    .isNotBlank()
                    .hasSize(32) // MD5 해시는 32자
                    .isEqualTo(messageId2);
        }

        @ParameterizedTest
        @DisplayName("다양한 타입의 response 데이터에 대해 메시지 ID를 생성한다")
        @MethodSource("coffeeshout.global.websocket.GameRecoveryServiceTest#다양한_response_데이터")
        void 다양한_타입의_response에_대해_ID를_생성한다(WebSocketResponse<?> response) {
            // given
            String destination = "/topic/room/ABCD";

            // when
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // then
            assertThat(messageId)
                    .isNotBlank()
                    .hasSize(32);
        }
    }

    static Stream<Arguments> 다양한_response_데이터() {
        return Stream.of(
                Arguments.of(WebSocketResponse.success("string data")),
                Arguments.of(WebSocketResponse.success(12345)),
                Arguments.of(WebSocketResponse.success(List.of("a", "b", "c"))),
                Arguments.of(WebSocketResponse.success(null)),
                Arguments.of(WebSocketResponse.error("에러 메시지"))
        );
    }

    @Nested
    @DisplayName("메시지 저장 테스트")
    class 메시지_저장 {

        @Test
        @DisplayName("메시지를 정상적으로 저장한다")
        void 메시지를_정상적으로_저장한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("game started");
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // when
            String streamId = gameRecoveryService.save(joinCode, destination, response, messageId);

            // then
            assertThat(streamId).isNotBlank().matches("\\d+-\\d+"); //Redis Stream ID 형식: 1234567890-0
        }

        @Test
        @DisplayName("중복 메시지 저장 시 기존 streamId를 반환한다 (Lua 스크립트 중복 방지)")
        void 중복_메시지_저장시_기존_streamId를_반환한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("game started");
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // when
            String streamId1 = gameRecoveryService.save(joinCode, destination, response, messageId);
            String streamId2 = gameRecoveryService.save(joinCode, destination, response, messageId);

            // then
            assertThat(streamId1).isEqualTo(streamId2);
        }

        @Test
        @DisplayName("다른 메시지는 다른 streamId로 저장된다")
        void 다른_메시지는_다른_streamId로_저장된다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response1 = WebSocketResponse.success("message1");
            WebSocketResponse<String> response2 = WebSocketResponse.success("message2");
            String messageId1 = gameRecoveryService.generateMessageId(destination, response1);
            String messageId2 = gameRecoveryService.generateMessageId(destination, response2);

            // when
            String streamId1 = gameRecoveryService.save(joinCode, destination, response1, messageId1);
            String streamId2 = gameRecoveryService.save(joinCode, destination, response2, messageId2);

            // then
            assertThat(streamId1).isNotEqualTo(streamId2);
        }

        @ParameterizedTest
        @DisplayName("여러 개의 메시지를 순차적으로 저장한다")
        @ValueSource(ints = {1, 3, 5, 10})
        void 여러_메시지를_순차적으로_저장한다(int messageCount) {
            // given
            String destination = "/topic/room/" + joinCode;

            // when & then
            String previousStreamId = "0-0";
            for (int i = 0; i < messageCount; i++) {
                WebSocketResponse<String> response = WebSocketResponse.success("message" + i);
                String messageId = gameRecoveryService.generateMessageId(destination, response);
                String streamId = gameRecoveryService.save(joinCode, destination, response, messageId);

                assertThat(streamId).isNotBlank();
                assertThat(streamId.compareTo(previousStreamId)).isGreaterThan(0);
                previousStreamId = streamId;
            }
        }

        @Test
        @DisplayName("idMapKey에 messageId와 streamId 매핑이 저장된다")
        void idMapKey에_매핑이_저장된다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("test");
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // when
            String streamId = gameRecoveryService.save(joinCode, destination, response, messageId);

            // then
            String idMapKey = String.format(ID_MAP_KEY_FORMAT, joinCode);
            String storedStreamId = stringRedisTemplate.opsForHash().get(idMapKey, messageId).toString();
            assertThat(storedStreamId).isEqualTo(streamId);
        }
    }

    @Nested
    @DisplayName("메시지 복구 테스트")
    class 메시지_복구 {

        @Test
        @DisplayName("lastStreamId 이후의 메시지들을 복구한다")
        void lastStreamId_이후의_메시지들을_복구한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            String streamId1 = saveMessage(destination, "message1");
            String streamId2 = saveMessage(destination, "message2");
            String streamId3 = saveMessage(destination, "message3");

            // when - streamId1 이후의 메시지 조회
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, streamId1);

            // then
            assertThat(messages).hasSize(2);
            assertThat(messages.get(0).streamId()).isEqualTo(streamId2);
            assertThat(messages.get(1).streamId()).isEqualTo(streamId3);
        }

        @Test
        @DisplayName("3개 이상의 메시지를 저장하고 복구한다")
        void 다수의_메시지를_저장하고_복구한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            int messageCount = 5;

            for (int i = 1; i <= messageCount; i++) {
                saveMessage(destination, "message" + i);
            }

            // when - 처음부터 모든 메시지 조회
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).hasSize(messageCount);
            for (int i = 0; i < messageCount; i++) {
                assertThat(messages.get(i).destination()).isEqualTo(destination);
            }
        }

        @Test
        @DisplayName("복구할 메시지가 없으면 빈 리스트를 반환한다")
        void 복구할_메시지가_없으면_빈_리스트를_반환한다() {
            // when
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).isEmpty();
        }

        @Test
        @DisplayName("마지막 메시지 이후에는 빈 리스트를 반환한다")
        void 마지막_메시지_이후에는_빈_리스트를_반환한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            String lastStreamId = saveMessage(destination, "last message");

            // when
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, lastStreamId);

            // then
            assertThat(messages).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 방의 메시지를 조회하면 빈 리스트를 반환한다")
        void 존재하지_않는_방의_메시지를_조회하면_빈_리스트를_반환한다() {
            // when
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(new JoinCode("ZZZZ"), "0-0");

            // then
            assertThat(messages).isEmpty();
        }

        @ParameterizedTest
        @DisplayName("다양한 lastStreamId로 메시지를 복구한다")
        @MethodSource("coffeeshout.global.websocket.GameRecoveryServiceTest#다양한_lastStreamId_시나리오")
        void 다양한_lastStreamId로_메시지를_복구한다(int skipCount, int expectedCount) {
            // given
            String destination = "/topic/room/" + joinCode;
            String[] streamIds = new String[5];
            for (int i = 0; i < 5; i++) {
                streamIds[i] = saveMessage(destination, "message" + i);
            }

            String lastStreamId = skipCount == 0 ? "0-0" : streamIds[skipCount - 1];

            // when
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, lastStreamId);

            // then
            assertThat(messages).hasSize(expectedCount);
        }

        @Test
        @DisplayName("복구된 메시지의 구조가 올바른지 확인한다")
        void 복구된_메시지의_구조가_올바르다() {
            // given
            String destination = "/topic/room/" + joinCode;
            String testData = "test data";
            saveMessage(destination, testData);

            // when
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).hasSize(1);
            RecoveryMessage recoveredMessage = messages.get(0);
            assertThat(recoveredMessage.streamId()).isNotBlank();
            assertThat(recoveredMessage.destination()).isEqualTo(destination);
            assertThat(recoveredMessage.response()).isNotNull();
            assertThat(recoveredMessage.response().success()).isTrue();
            assertThat(recoveredMessage.timestamp()).isPositive();
        }

        private String saveMessage(String destination, String data) {
            WebSocketResponse<String> response = WebSocketResponse.success(data);
            String messageId = gameRecoveryService.generateMessageId(destination, response);
            return gameRecoveryService.save(joinCode, destination, response, messageId);
        }
    }

    static Stream<Arguments> 다양한_lastStreamId_시나리오() {
        return Stream.of(
                Arguments.of(0, 5),  // 처음부터 조회 -> 5개
                Arguments.of(1, 4),  // 1개 건너뛰고 조회 -> 4개
                Arguments.of(2, 3),  // 2개 건너뛰고 조회 -> 3개
                Arguments.of(4, 1),  // 4개 건너뛰고 조회 -> 1개
                Arguments.of(5, 0)   // 전부 건너뛰고 조회 -> 0개
        );
    }

    @Nested
    @DisplayName("방 정리 테스트")
    class 방_정리 {

        @Test
        @DisplayName("cleanup 호출 시 해당 방의 Redis 키가 삭제된다")
        void cleanup_호출시_Redis_키가_삭제된다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("test");
            String messageId = gameRecoveryService.generateMessageId(destination, response);
            gameRecoveryService.save(joinCode, destination, response, messageId);

            String streamKey = String.format(STREAM_KEY_FORMAT, joinCode);
            String idMapKey = String.format(ID_MAP_KEY_FORMAT, joinCode);

            // 저장 확인
            assertThat(stringRedisTemplate.hasKey(streamKey)).isTrue();
            assertThat(stringRedisTemplate.hasKey(idMapKey)).isTrue();

            // when
            gameRecoveryService.cleanup(joinCode);

            // then
            assertThat(stringRedisTemplate.hasKey(streamKey)).isFalse();
            assertThat(stringRedisTemplate.hasKey(idMapKey)).isFalse();
        }

        @Test
        @DisplayName("cleanup 후 메시지 복구 시 빈 리스트를 반환한다")
        void cleanup_후_메시지_복구시_빈_리스트를_반환한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("test");
            String messageId = gameRecoveryService.generateMessageId(destination, response);
            gameRecoveryService.save(joinCode, destination, response, messageId);

            // when
            gameRecoveryService.cleanup(joinCode);
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 방을 cleanup해도 예외가 발생하지 않는다")
        void 존재하지_않는_방_cleanup해도_예외가_발생하지_않는다() {
            // given
            JoinCode nonExistentJoinCode = new JoinCode("ZZZZ");

            // when & then - 예외 없이 정상 수행
            gameRecoveryService.cleanup(nonExistentJoinCode);
        }

        @ParameterizedTest
        @DisplayName("다양한 joinCode에 대해 cleanup이 정상 동작한다")
        @ValueSource(strings = {"ABCD", "XYZ9", "ABCF"})
        void 다양한_joinCode에_대해_cleanup이_정상_동작한다(String code) {
            // given
            JoinCode jc = new JoinCode(code);
            cleanupRedis(jc);
            String destination = "/topic/room/" + code;
            WebSocketResponse<String> response = WebSocketResponse.success("test");
            String messageId = gameRecoveryService.generateMessageId(destination, response);
            gameRecoveryService.save(jc, destination, response, messageId);

            // when
            gameRecoveryService.cleanup(jc);

            // then
            String streamKey = String.format(STREAM_KEY_FORMAT, code);
            String idMapKey = String.format(ID_MAP_KEY_FORMAT, code);
            assertThat(stringRedisTemplate.hasKey(streamKey)).isFalse();
            assertThat(stringRedisTemplate.hasKey(idMapKey)).isFalse();
        }
    }

    @Nested
    @DisplayName("TTL 검증 테스트")
    class TTL_검증 {

        @Autowired
        @Qualifier("redisObjectMapper")
        ObjectMapper objectMapper;

        private static final JoinCode TTL_TEST_JOIN_CODE = new JoinCode("TTXV");
        private static final int MAX_LENGTH = 100;
        private static final int SHORT_STREAM_TTL = 5;
        private static final int SHORT_DEDUP_TTL = 2;

        @BeforeEach
        void cleanupTtlTestData() {
            cleanupRedis(TTL_TEST_JOIN_CODE);
        }

        @Test
        @DisplayName("idMapKey(중복 방지용)는 짧은 TTL 후 만료된다")
        void idMapKey는_짧은_TTL_후_만료된다() {
            // given - 짧은 dedup TTL(2초)로 GameRecoveryService 생성
            GameRecoveryService shortTtlService = new GameRecoveryService(
                    stringRedisTemplate,
                    objectMapper,
                    MAX_LENGTH,
                    SHORT_STREAM_TTL,
                    SHORT_DEDUP_TTL
            );

            String destination = "/topic/room/" + TTL_TEST_JOIN_CODE;
            WebSocketResponse<String> response = WebSocketResponse.success("ttl test");
            String messageId = shortTtlService.generateMessageId(destination, response);

            // when - 메시지 저장
            shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response, messageId);

            String streamKey = String.format(STREAM_KEY_FORMAT, TTL_TEST_JOIN_CODE);
            String idMapKey = String.format(ID_MAP_KEY_FORMAT, TTL_TEST_JOIN_CODE);

            // 저장 직후에는 둘 다 존재
            assertThat(stringRedisTemplate.hasKey(streamKey)).isTrue();
            assertThat(stringRedisTemplate.hasKey(idMapKey)).isTrue();

            // then - 3초 후 idMapKey만 제거되고 streamKey는 유지됨
            await().atMost(4, SECONDS)
                    .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(stringRedisTemplate.hasKey(idMapKey)).isFalse();
                        assertThat(stringRedisTemplate.hasKey(streamKey)).isTrue();
                    });
        }

        @Test
        @DisplayName("중복 방지 TTL 만료 후 같은 메시지를 다시 저장할 수 있다")
        void 중복_방지_TTL_만료_후_같은_메시지를_다시_저장할_수_있다() {
            // given
            GameRecoveryService shortTtlService = new GameRecoveryService(
                    stringRedisTemplate,
                    objectMapper,
                    MAX_LENGTH,
                    SHORT_STREAM_TTL,
                    SHORT_DEDUP_TTL
            );

            String destination = "/topic/room/" + TTL_TEST_JOIN_CODE;
            WebSocketResponse<String> response = WebSocketResponse.success("same message");
            String messageId = shortTtlService.generateMessageId(destination, response);

            // when - 첫 번째 저장
            String streamId1 = shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response, messageId);

            // dedup TTL 만료 대기 (2초 + 여유)
            await().atMost(4, SECONDS)
                    .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .until(() -> {
                        String idMapKey = String.format(ID_MAP_KEY_FORMAT, TTL_TEST_JOIN_CODE);
                        return !stringRedisTemplate.hasKey(idMapKey);
                    });

            // 같은 메시지를 다시 저장
            String streamId2 = shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response, messageId);

            // then - 다른 streamId로 저장됨 (중복 방지가 만료되었으므로)
            assertThat(streamId1).isNotEqualTo(streamId2);

            // 복구 시 2개의 메시지가 조회됨
            List<RecoveryMessage> messages = shortTtlService.getMessagesSince(TTL_TEST_JOIN_CODE, "0-0");
            assertThat(messages).hasSize(2);
        }

        @Test
        @DisplayName("중복 방지 TTL 내에서는 같은 메시지가 중복 저장되지 않는다")
        void 중복_방지_TTL_내에서는_같은_메시지가_중복_저장되지_않는다() {
            // given
            GameRecoveryService shortTtlService = new GameRecoveryService(
                    stringRedisTemplate,
                    objectMapper,
                    MAX_LENGTH,
                    SHORT_STREAM_TTL,
                    SHORT_DEDUP_TTL
            );

            String destination = "/topic/room/" + TTL_TEST_JOIN_CODE;
            WebSocketResponse<String> response = WebSocketResponse.success("same message");
            String messageId = shortTtlService.generateMessageId(destination, response);

            // when - 빠르게 연속 저장
            String streamId1 = shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response, messageId);
            String streamId2 = shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response, messageId);

            // then - 같은 streamId 반환 (중복 방지 동작)
            assertThat(streamId1).isEqualTo(streamId2);

            List<RecoveryMessage> messages = shortTtlService.getMessagesSince(TTL_TEST_JOIN_CODE, "0-0");
            assertThat(messages).hasSize(1);
        }

        @Test
        @DisplayName("streamKey와 idMapKey의 TTL이 서로 다르게 설정된다")
        void streamKey와_idMapKey의_TTL이_서로_다르게_설정된다() {
            // given
            GameRecoveryService shortTtlService = new GameRecoveryService(
                    stringRedisTemplate,
                    objectMapper,
                    MAX_LENGTH,
                    SHORT_STREAM_TTL,
                    SHORT_DEDUP_TTL
            );

            String destination = "/topic/room/" + TTL_TEST_JOIN_CODE;
            WebSocketResponse<String> response = WebSocketResponse.success("ttl test");
            String messageId = shortTtlService.generateMessageId(destination, response);

            // when
            shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response, messageId);

            // then
            String streamKey = String.format(STREAM_KEY_FORMAT, TTL_TEST_JOIN_CODE);
            String idMapKey = String.format(ID_MAP_KEY_FORMAT, TTL_TEST_JOIN_CODE);

            Long streamKeyTtl = stringRedisTemplate.getExpire(streamKey);
            Long idMapKeyTtl = stringRedisTemplate.getExpire(idMapKey);

            // idMapKey는 짧은 TTL (2초 이하)
            assertThat(idMapKeyTtl).isGreaterThan(0L).isLessThanOrEqualTo((long) SHORT_DEDUP_TTL);

            // streamKey는 긴 TTL (5초 이하, idMapKey보다 큼)
            assertThat(streamKeyTtl).isGreaterThan(0L).isLessThanOrEqualTo((long) SHORT_STREAM_TTL);
            assertThat(streamKeyTtl).isGreaterThan(idMapKeyTtl);
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class 엣지_케이스 {

        @ParameterizedTest
        @DisplayName("빈 문자열 데이터도 정상적으로 처리한다")
        @ValueSource(strings = {"", " ", "   "})
        void 빈_문자열_데이터도_정상적으로_처리한다(String data) {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success(data);
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // when
            String streamId = gameRecoveryService.save(joinCode, destination, response, messageId);

            // then
            assertThat(streamId).isNotBlank();

            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");
            assertThat(messages).hasSize(1);
        }

        @Test
        @DisplayName("특수문자가 포함된 데이터도 정상적으로 처리한다")
        void 특수문자가_포함된_데이터도_정상적으로_처리한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            String dataWithSpecialChars = "한글 데이터 🎮 <script>alert('xss')</script> \"quoted\"";
            WebSocketResponse<String> response = WebSocketResponse.success(dataWithSpecialChars);
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // when
            String streamId = gameRecoveryService.save(joinCode, destination, response, messageId);

            // then
            assertThat(streamId).isNotBlank();

            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");
            assertThat(messages).hasSize(1);
        }

        @Test
        @DisplayName("대량의 메시지를 저장하고 복구할 수 있다")
        void 대량의_메시지를_저장하고_복구한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            int largeMessageCount = 100;

            for (int i = 0; i < largeMessageCount; i++) {
                WebSocketResponse<String> response = WebSocketResponse.success("message" + i);
                String messageId = gameRecoveryService.generateMessageId(destination, response);
                gameRecoveryService.save(joinCode, destination, response, messageId);
            }

            // when
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            // maxLength 설정에 따라 일부만 유지될 수 있음
            assertThat(messages).isNotEmpty();
        }

        @Test
        @DisplayName("에러 응답도 정상적으로 저장하고 복구한다")
        void 에러_응답도_정상적으로_저장하고_복구한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> errorResponse = WebSocketResponse.error("게임 시작 실패");
            String messageId = gameRecoveryService.generateMessageId(destination, errorResponse);

            // when
            String streamId = gameRecoveryService.save(joinCode, destination, errorResponse, messageId);
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(streamId).isNotBlank();
            assertThat(messages).hasSize(1);
            assertThat(messages.getFirst().response().success()).isFalse();
            assertThat(messages.getFirst().response().errorMessage()).isEqualTo("게임 시작 실패");
        }

        @Test
        @DisplayName("복잡한 객체 데이터도 정상적으로 처리한다")
        void 복잡한_객체_데이터도_정상적으로_처리한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            List<String> complexData = List.of("player1", "player2", "player3");
            WebSocketResponse<List<String>> response = WebSocketResponse.success(complexData);
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // when
            String streamId = gameRecoveryService.save(joinCode, destination, response, messageId);

            // then
            assertThat(streamId).isNotBlank();
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");
            assertThat(messages).hasSize(1);
        }

        @Test
        @DisplayName("서로 다른 방의 메시지는 독립적으로 관리된다")
        void 서로_다른_방의_메시지는_독립적으로_관리된다() {
            // given
            JoinCode joinCode1 = new JoinCode("ABC3");
            JoinCode joinCode2 = new JoinCode("ABC4");
            cleanupRedis(joinCode1);
            cleanupRedis(joinCode2);

            String destination1 = "/topic/room/" + joinCode1;
            String destination2 = "/topic/room/" + joinCode2;

            WebSocketResponse<String> response1 = WebSocketResponse.success("room1 message");
            WebSocketResponse<String> response2 = WebSocketResponse.success("room2 message");

            String messageId1 = gameRecoveryService.generateMessageId(destination1, response1);
            String messageId2 = gameRecoveryService.generateMessageId(destination2, response2);

            gameRecoveryService.save(joinCode1, destination1, response1, messageId1);
            gameRecoveryService.save(joinCode2, destination2, response2, messageId2);

            // when
            gameRecoveryService.cleanup(joinCode1);

            // then
            List<RecoveryMessage> room1Messages = gameRecoveryService.getMessagesSince(joinCode1, "0-0");
            List<RecoveryMessage> room2Messages = gameRecoveryService.getMessagesSince(joinCode2, "0-0");

            assertThat(room1Messages).isEmpty();
            assertThat(room2Messages).hasSize(1);

            // cleanup
            cleanupRedis(joinCode2);
        }
    }

    @Nested
    @DisplayName("실제 MessagePublisher destination 테스트")
    class 실제_MessagePublisher_destination_테스트 {

        @ParameterizedTest
        @DisplayName("RoomMessagePublisher의 모든 destination에서 메시지가 정상 저장된다")
        @MethodSource("coffeeshout.global.websocket.GameRecoveryServiceTest#roomMessagePublisherDestinations")
        void RoomMessagePublisher_destination_저장_테스트(String destinationFormat, String testData) {
            // given
            String destination = String.format(destinationFormat, joinCode);
            WebSocketResponse<String> response = WebSocketResponse.success(testData);
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // when
            String streamId = gameRecoveryService.save(joinCode, destination, response, messageId);

            // then
            assertThat(streamId).isNotBlank();
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");
            assertThat(messages).hasSize(1);
            assertThat(messages.getFirst().destination()).isEqualTo(destination);

            // cleanup for next test
            cleanupRedis(joinCode);
        }

        @ParameterizedTest
        @DisplayName("CardGameMessagePublisher의 모든 destination에서 메시지가 정상 저장된다")
        @MethodSource("coffeeshout.global.websocket.GameRecoveryServiceTest#cardGameMessagePublisherDestinations")
        void CardGameMessagePublisher_destination_저장_테스트(String destinationFormat, String testData) {
            // given
            String destination = String.format(destinationFormat, joinCode);
            WebSocketResponse<String> response = WebSocketResponse.success(testData);
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // when
            String streamId = gameRecoveryService.save(joinCode, destination, response, messageId);

            // then
            assertThat(streamId).isNotBlank();
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");
            assertThat(messages).hasSize(1);
            assertThat(messages.getFirst().destination()).isEqualTo(destination);

            // cleanup for next test
            cleanupRedis(joinCode);
        }

        @ParameterizedTest
        @DisplayName("RacingGameMessagePublisher의 모든 destination에서 메시지가 정상 저장된다")
        @MethodSource("coffeeshout.global.websocket.GameRecoveryServiceTest#racingGameMessagePublisherDestinations")
        void RacingGameMessagePublisher_destination_저장_테스트(String destinationFormat, String testData) {
            // given
            String destination = String.format(destinationFormat, joinCode);
            WebSocketResponse<String> response = WebSocketResponse.success(testData);
            String messageId = gameRecoveryService.generateMessageId(destination, response);

            // when
            String streamId = gameRecoveryService.save(joinCode, destination, response, messageId);

            // then
            assertThat(streamId).isNotBlank();
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");
            assertThat(messages).hasSize(1);
            assertThat(messages.getFirst().destination()).isEqualTo(destination);

            // cleanup for next test
            cleanupRedis(joinCode);
        }

        @Test
        @DisplayName("같은 방의 다양한 destination 메시지들이 모두 복구된다")
        void 같은_방의_다양한_destination_메시지들이_모두_복구된다() {
            // given - 다양한 destination으로 메시지 저장
            String playerListDest = String.format(PLAYER_LIST_TOPIC_FORMAT, joinCode);
            String miniGameDest = String.format(MINI_GAME_TOPIC_FORMAT, joinCode);
            String rouletteDest = String.format(ROULETTE_TOPIC_FORMAT, joinCode);
            String cardGameDest = String.format(CARD_GAME_STATE_DESTINATION_FORMAT, joinCode);
            String racingGameDest = String.format(RACING_GAME_PLAYERS_POSITION_DESTINATION_FORMAT, joinCode);

            saveMessage(playerListDest, "player list data");
            saveMessage(miniGameDest, "minigame data");
            saveMessage(rouletteDest, "roulette data");
            saveMessage(cardGameDest, "card game data");
            saveMessage(racingGameDest, "racing game data");

            // when
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).hasSize(5);

            // 각 destination이 올바르게 저장되었는지 확인
            assertThat(messages).extracting(RecoveryMessage::destination)
                    .containsExactlyInAnyOrder(
                            playerListDest,
                            miniGameDest,
                            rouletteDest,
                            cardGameDest,
                            racingGameDest
                    );
        }

        @Test
        @DisplayName("destination에 포함된 joinCode와 저장된 joinCode가 일치하는 메시지만 복구된다")
        void destination_joinCode와_저장된_joinCode가_일치하는_메시지만_복구된다() {
            // given
            JoinCode joinCode1 = new JoinCode("ABC3");
            JoinCode joinCode2 = new JoinCode("ABC4");
            cleanupRedis(joinCode1);
            cleanupRedis(joinCode2);

            // joinCode1 방에 메시지 저장
            String dest1 = String.format(PLAYER_LIST_TOPIC_FORMAT, joinCode1);
            saveMessageToRoom(joinCode1, dest1, "room1 player list");

            // joinCode2 방에 메시지 저장
            String dest2 = String.format(PLAYER_LIST_TOPIC_FORMAT, joinCode2);
            saveMessageToRoom(joinCode2, dest2, "room2 player list");

            // when - joinCode1으로만 복구
            List<RecoveryMessage> room1Messages = gameRecoveryService.getMessagesSince(joinCode1, "0-0");
            List<RecoveryMessage> room2Messages = gameRecoveryService.getMessagesSince(joinCode2, "0-0");

            // then
            assertThat(room1Messages).hasSize(1);
            assertThat(room1Messages.getFirst().destination()).contains(joinCode1.getValue());

            assertThat(room2Messages).hasSize(1);
            assertThat(room2Messages.getFirst().destination()).contains(joinCode2.getValue());

            // cleanup
            cleanupRedis(joinCode1);
            cleanupRedis(joinCode2);
        }

        @Test
        @DisplayName("복구된 메시지의 destination에서 joinCode를 추출할 수 있다")
        void 복구된_메시지의_destination에서_joinCode를_추출할_수_있다() {
            // given
            JoinCode testJoinCode = new JoinCode("XYZ7");
            cleanupRedis(testJoinCode);

            String destination = String.format(ROULETTE_TOPIC_FORMAT, testJoinCode);
            saveMessageToRoom(testJoinCode, destination, "roulette winner");

            // when
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(testJoinCode, "0-0");

            // then
            assertThat(messages).hasSize(1);
            String recoveredDestination = messages.getFirst().destination();

            // destination에서 joinCode 추출 검증
            assertThat(recoveredDestination).isEqualTo("/topic/room/" + testJoinCode + "/roulette");
            assertThat(recoveredDestination).contains(testJoinCode.getValue());

            // 정규식으로 joinCode 추출
            String extractedJoinCode = recoveredDestination.split("/")[3];
            assertThat(extractedJoinCode).isEqualTo(testJoinCode.getValue());

            // cleanup
            cleanupRedis(testJoinCode);
        }

        @ParameterizedTest
        @DisplayName("모든 MessagePublisher destination 형식에서 joinCode가 올바르게 포함된다")
        @MethodSource("coffeeshout.global.websocket.GameRecoveryServiceTest#allDestinationFormats")
        void 모든_destination_형식에서_joinCode가_올바르게_포함된다(String destinationFormat) {
            // given
            JoinCode testJoinCode = new JoinCode("T3ST");
            cleanupRedis(testJoinCode);

            String destination = String.format(destinationFormat, testJoinCode);
            saveMessageToRoom(testJoinCode, destination, "test data");

            // when
            List<RecoveryMessage> messages = gameRecoveryService.getMessagesSince(testJoinCode, "0-0");

            // then
            assertThat(messages).hasSize(1);
            RecoveryMessage message = messages.getFirst();

            // destination 검증
            assertThat(message.destination()).isEqualTo(destination);
            assertThat(message.destination()).contains(testJoinCode.getValue());

            // /topic/room/{joinCode} 또는 /topic/room/{joinCode}/... 형식 검증
            assertThat(message.destination()).startsWith("/topic/room/" + testJoinCode);

            // cleanup
            cleanupRedis(testJoinCode);
        }

        private void saveMessage(String destination, String data) {
            WebSocketResponse<String> response = WebSocketResponse.success(data);
            String messageId = gameRecoveryService.generateMessageId(destination, response);
            gameRecoveryService.save(joinCode, destination, response, messageId);
        }

        private void saveMessageToRoom(JoinCode roomJoinCode, String destination, String data) {
            WebSocketResponse<String> response = WebSocketResponse.success(data);
            String messageId = gameRecoveryService.generateMessageId(destination, response);
            gameRecoveryService.save(roomJoinCode, destination, response, messageId);
        }
    }

    static Stream<Arguments> roomMessagePublisherDestinations() {
        return Stream.of(
                Arguments.of(PLAYER_LIST_TOPIC_FORMAT, "player list"),
                Arguments.of(MINI_GAME_TOPIC_FORMAT, "minigame list"),
                Arguments.of(ROULETTE_TOPIC_FORMAT, "roulette state"),
                Arguments.of(WINNER_TOPIC_FORMAT, "winner info"),
                Arguments.of(QR_CODE_TOPIC_FORMAT, "qr code status")
        );
    }

    static Stream<Arguments> cardGameMessagePublisherDestinations() {
        return Stream.of(
                Arguments.of(CARD_GAME_STATE_DESTINATION_FORMAT, "card game state"),
                Arguments.of(GAME_START_DESTINATION_FORMAT, "game start")
        );
    }

    static Stream<Arguments> racingGameMessagePublisherDestinations() {
        return Stream.of(
                Arguments.of(RACING_GAME_PLAYERS_POSITION_DESTINATION_FORMAT, "runner positions"),
                Arguments.of(RACING_GAME_STATE_DESTINATION_FORMAT, "racing game state")
        );
    }

    static Stream<String> allDestinationFormats() {
        return Stream.of(
                // RoomMessagePublisher
                PLAYER_LIST_TOPIC_FORMAT,
                MINI_GAME_TOPIC_FORMAT,
                ROULETTE_TOPIC_FORMAT,
                WINNER_TOPIC_FORMAT,
                QR_CODE_TOPIC_FORMAT,
                // CardGameMessagePublisher
                CARD_GAME_STATE_DESTINATION_FORMAT,
                GAME_START_DESTINATION_FORMAT,
                // RacingGameMessagePublisher
                RACING_GAME_PLAYERS_POSITION_DESTINATION_FORMAT,
                RACING_GAME_STATE_DESTINATION_FORMAT
        );
    }
}
