package coffeeshout.websocket;

import static coffeeshout.websocket.WsRecoveryService.ID_MAP_KEY_FORMAT;
import static coffeeshout.websocket.WsRecoveryService.STREAM_KEY_FORMAT;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import coffeeshout.WebsocketModuleIntegrationTest;
import coffeeshout.websocket.ui.WebSocketResponse;
import coffeeshout.websocket.ui.dto.RecoveryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;

class WsRecoveryServiceTest extends WebsocketModuleIntegrationTest {

    @Autowired
    WsRecoveryService wsRecoveryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;


    private String joinCode;

    @BeforeEach
    void setUp() {
        joinCode = "ABCD";
        // 테스트 전 Redis 정리
        cleanupRedis(joinCode);
    }

    private void cleanupRedis(String joinCode) {
        String streamKey = String.format(STREAM_KEY_FORMAT, joinCode);
        String idMapKey = String.format(ID_MAP_KEY_FORMAT, joinCode);
        stringRedisTemplate.delete(List.of(streamKey, idMapKey));
    }

    @Nested
    class 메시지_저장 {

        @Test
        void 메시지를_정상적으로_저장한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("game started");

            // when
            String streamId = wsRecoveryService.save(joinCode, destination, response);

            // then
            assertThat(streamId).isNotBlank().matches("\\d+-\\d+"); //Redis Stream ID 형식: 1234567890-0
        }

        @Test
        void 중복_메시지_저장시_기존_streamId를_반환한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("game started");

            // when
            String streamId1 = wsRecoveryService.save(joinCode, destination, response);
            String streamId2 = wsRecoveryService.save(joinCode, destination, response);

            // then
            assertThat(streamId1).isEqualTo(streamId2);
        }

        @Test
        void 다른_메시지는_다른_streamId로_저장된다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response1 = WebSocketResponse.success("message1");
            WebSocketResponse<String> response2 = WebSocketResponse.success("message2");

            // when
            String streamId1 = wsRecoveryService.save(joinCode, destination, response1);
            String streamId2 = wsRecoveryService.save(joinCode, destination, response2);

            // then
            assertThat(streamId1).isNotEqualTo(streamId2);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 3, 5, 10})
        void 여러_메시지를_순차적으로_저장한다(int messageCount) {
            // given
            String destination = "/topic/room/" + joinCode;

            // when & then
            String previousStreamId = "0-0";
            for (int i = 0; i < messageCount; i++) {
                WebSocketResponse<String> response = WebSocketResponse.success("message" + i);
                String streamId = wsRecoveryService.save(joinCode, destination, response);

                assertThat(streamId).isNotBlank();
                assertThat(streamId.compareTo(previousStreamId)).isGreaterThan(0);
                previousStreamId = streamId;
            }
        }

        @Test
        void idMapKey에_매핑이_저장된다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("test");

            // when
            wsRecoveryService.save(joinCode, destination, response);

            // then
            String idMapKey = String.format(ID_MAP_KEY_FORMAT, joinCode);
            assertThat(stringRedisTemplate.hasKey(idMapKey)).isTrue();
            // idMapKey에 값이 존재하는지 확인
            Long size = stringRedisTemplate.opsForHash().size(idMapKey);
            assertThat(size).isGreaterThan(0);
        }
    }

    @Nested
    class 메시지_복구 {

        @Test
        void lastStreamId_이후의_메시지들을_복구한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            String streamId1 = saveMessage(destination, "message1");
            String streamId2 = saveMessage(destination, "message2");
            String streamId3 = saveMessage(destination, "message3");

            // when - streamId1 이후의 메시지 조회
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, streamId1);

            // then
            assertThat(messages).hasSize(2);
            assertThat(messages.getFirst().streamId()).isEqualTo(streamId2);
            assertThat(messages.get(1).streamId()).isEqualTo(streamId3);
        }

        @Test
        void 다수의_메시지를_저장하고_복구한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            int messageCount = 5;

            for (int i = 1; i <= messageCount; i++) {
                saveMessage(destination, "message" + i);
            }

            // when - 처음부터 모든 메시지 조회
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).hasSize(messageCount);
            for (int i = 0; i < messageCount; i++) {
                assertThat(messages.get(i).destination()).isEqualTo(destination);
            }
        }

        @Test
        void 복구할_메시지가_없으면_빈_리스트를_반환한다() {
            // when
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).isEmpty();
        }

        @Test
        void 마지막_메시지_이후에는_빈_리스트를_반환한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            String lastStreamId = saveMessage(destination, "last message");

            // when
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, lastStreamId);

            // then
            assertThat(messages).isEmpty();
        }

        @Test
        void 존재하지_않는_방의_메시지를_조회하면_빈_리스트를_반환한다() {
            // when
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince("ZZZZ", "0-0");

            // then
            assertThat(messages).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("coffeeshout.websocket.WsRecoveryServiceTest#다양한_lastStreamId_시나리오")
        void 다양한_lastStreamId로_메시지를_복구한다(int skipCount, int expectedCount) {
            // given
            String destination = "/topic/room/" + joinCode;
            String[] streamIds = new String[5];
            for (int i = 0; i < 5; i++) {
                streamIds[i] = saveMessage(destination, "message" + i);
            }

            String lastStreamId = skipCount == 0 ? "0-0" : streamIds[skipCount - 1];

            // when
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, lastStreamId);

            // then
            assertThat(messages).hasSize(expectedCount);
        }

        @Test
        void 복구된_메시지의_구조가_올바르다() {
            // given
            String destination = "/topic/room/" + joinCode;
            String testData = "test data";
            saveMessage(destination, testData);

            // when
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).hasSize(1);
            RecoveryMessage recoveredMessage = messages.getFirst();
            assertThat(recoveredMessage.streamId()).isNotBlank();
            assertThat(recoveredMessage.destination()).isEqualTo(destination);
            assertThat(recoveredMessage.response()).isNotNull();
            assertThat(recoveredMessage.response().success()).isTrue();
            assertThat(recoveredMessage.timestamp()).isPositive();
        }

        @Test
        void 여러_메시지_저장_후_첫번째_streamId로_나머지_복구한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            int totalMessages = 5;
            List<String> streamIds = new ArrayList<>();

            for (int i = 0; i < totalMessages; i++) {
                String data = "message" + i;
                String streamId = saveMessage(destination, data);
                streamIds.add(streamId);
            }

            // when - 첫 번째 streamId 이후의 메시지들 복구
            String firstStreamId = streamIds.getFirst();
            List<RecoveryMessage> recoveredMessages = wsRecoveryService.getMessagesSince(joinCode, firstStreamId);

            // then
            assertThat(recoveredMessages).hasSize(totalMessages - 1);

            // 복구된 메시지들의 streamId가 첫 번째 이후의 것들인지 확인
            for (int i = 0; i < recoveredMessages.size(); i++) {
                RecoveryMessage message = recoveredMessages.get(i);
                assertThat(message.streamId()).isEqualTo(streamIds.get(i + 1));
                assertThat(message.destination()).isEqualTo(destination);
                assertThat(message.response().success()).isTrue();
            }
        }

        @Test
        void 같은_방의_다양한_destination에_저장된_메시지들이_모두_복구된다() {
            // given
            String dest1 = "/topic/room/" + joinCode + "/players";
            String dest2 = "/topic/room/" + joinCode + "/minigame";
            String dest3 = "/topic/room/" + joinCode + "/roulette";
            String dest4 = "/topic/room/" + joinCode + "/cardgame";
            String dest5 = "/topic/room/" + joinCode + "/racinggame";

            saveMessage(dest1, "data1");
            saveMessage(dest2, "data2");
            saveMessage(dest3, "data3");
            saveMessage(dest4, "data4");
            saveMessage(dest5, "data5");

            // when
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).hasSize(5)
                    .extracting(RecoveryMessage::destination)
                    .containsExactlyInAnyOrder(dest1, dest2, dest3, dest4, dest5);
        }

        private String saveMessage(String destination, String data) {
            WebSocketResponse<String> response = WebSocketResponse.success(data);
            return wsRecoveryService.save(joinCode, destination, response);
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
    class 방_정리 {

        @Test
        void cleanup_호출시_Redis_키가_삭제된다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("test");
            wsRecoveryService.save(joinCode, destination, response);

            String streamKey = String.format(STREAM_KEY_FORMAT, joinCode);
            String idMapKey = String.format(ID_MAP_KEY_FORMAT, joinCode);

            // 저장 확인
            assertThat(stringRedisTemplate.hasKey(streamKey)).isTrue();
            assertThat(stringRedisTemplate.hasKey(idMapKey)).isTrue();

            // when
            wsRecoveryService.cleanup(joinCode);

            // then
            assertThat(stringRedisTemplate.hasKey(streamKey)).isFalse();
            assertThat(stringRedisTemplate.hasKey(idMapKey)).isFalse();
        }

        @Test
        void cleanup_후_메시지_복구시_빈_리스트를_반환한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success("test");
            wsRecoveryService.save(joinCode, destination, response);

            // when
            wsRecoveryService.cleanup(joinCode);
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(messages).isEmpty();
        }

        @Test
        void 존재하지_않는_방_cleanup해도_예외가_발생하지_않는다() {
            // given
            String nonExistentJoinCode = "ZZZZ";

            // when & then - 예외 없이 정상 수행
            assertThatCode(() -> wsRecoveryService.cleanup(nonExistentJoinCode))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABCD", "XYZ9", "ABCF"})
        void 다양한_joinCode에_대해_cleanup이_정상_동작한다(String code) {
            // given
            String jc = code;
            cleanupRedis(jc);
            String destination = "/topic/room/" + code;
            WebSocketResponse<String> response = WebSocketResponse.success("test");
            wsRecoveryService.save(jc, destination, response);

            // when
            wsRecoveryService.cleanup(jc);

            // then
            String streamKey = String.format(STREAM_KEY_FORMAT, code);
            String idMapKey = String.format(ID_MAP_KEY_FORMAT, code);
            assertThat(stringRedisTemplate.hasKey(streamKey)).isFalse();
            assertThat(stringRedisTemplate.hasKey(idMapKey)).isFalse();
        }
    }

    @Nested
    class TTL_검증 {

        @Autowired
        @Qualifier("redisObjectMapper")
        ObjectMapper objectMapper;

        private static final String TTL_TEST_JOIN_CODE = "TTXV";
        private static final int MAX_LENGTH = 100;
        private static final int SHORT_STREAM_TTL = 5;
        private static final int SHORT_DEDUP_TTL = 2;

        @BeforeEach
        void cleanupTtlTestData() {
            cleanupRedis(TTL_TEST_JOIN_CODE);
        }

        @Test
        void idMapKey는_짧은_TTL_후_만료된다() {
            // given - 짧은 dedup TTL(2초)로 GameRecoveryService 생성
            WsRecoveryService shortTtlService = new WsRecoveryService(
                    stringRedisTemplate,
                    objectMapper,
                    MAX_LENGTH,
                    SHORT_STREAM_TTL,
                    SHORT_DEDUP_TTL
            );

            String destination = "/topic/room/" + TTL_TEST_JOIN_CODE;
            WebSocketResponse<String> response = WebSocketResponse.success("ttl test");

            // when - 메시지 저장
            shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response);

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
        void 중복_방지_TTL_만료_후_같은_메시지를_다시_저장할_수_있다() {
            // given
            WsRecoveryService shortTtlService = new WsRecoveryService(
                    stringRedisTemplate,
                    objectMapper,
                    MAX_LENGTH,
                    SHORT_STREAM_TTL,
                    SHORT_DEDUP_TTL
            );

            String destination = "/topic/room/" + TTL_TEST_JOIN_CODE;
            WebSocketResponse<String> response = WebSocketResponse.success("same message");

            // when - 첫 번째 저장
            String streamId1 = shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response);

            // dedup TTL 만료 대기 (2초 + 여유)
            await().atMost(4, SECONDS)
                    .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .until(() -> {
                        String idMapKey = String.format(ID_MAP_KEY_FORMAT, TTL_TEST_JOIN_CODE);
                        return !stringRedisTemplate.hasKey(idMapKey);
                    });

            // 같은 메시지를 다시 저장
            String streamId2 = shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response);

            // then - 다른 streamId로 저장됨 (중복 방지가 만료되었으므로)
            assertThat(streamId1).isNotEqualTo(streamId2);

            // 복구 시 2개의 메시지가 조회됨
            List<RecoveryMessage> messages = shortTtlService.getMessagesSince(TTL_TEST_JOIN_CODE, "0-0");
            assertThat(messages).hasSize(2);
        }

        @Test
        void 중복_방지_TTL_내에서는_같은_메시지가_중복_저장되지_않는다() {
            // given
            WsRecoveryService shortTtlService = new WsRecoveryService(
                    stringRedisTemplate,
                    objectMapper,
                    MAX_LENGTH,
                    SHORT_STREAM_TTL,
                    SHORT_DEDUP_TTL
            );

            String destination = "/topic/room/" + TTL_TEST_JOIN_CODE;
            WebSocketResponse<String> response = WebSocketResponse.success("same message");

            // when - 빠르게 연속 저장
            String streamId1 = shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response);
            String streamId2 = shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response);

            // then - 같은 streamId 반환 (중복 방지 동작)
            assertThat(streamId1).isEqualTo(streamId2);

            List<RecoveryMessage> messages = shortTtlService.getMessagesSince(TTL_TEST_JOIN_CODE, "0-0");
            assertThat(messages).hasSize(1);
        }

        @Test
        void streamKey와_idMapKey의_TTL이_서로_다르게_설정된다() {
            // given
            WsRecoveryService shortTtlService = new WsRecoveryService(
                    stringRedisTemplate,
                    objectMapper,
                    MAX_LENGTH,
                    SHORT_STREAM_TTL,
                    SHORT_DEDUP_TTL
            );

            String destination = "/topic/room/" + TTL_TEST_JOIN_CODE;
            WebSocketResponse<String> response = WebSocketResponse.success("ttl test");

            // when
            shortTtlService.save(TTL_TEST_JOIN_CODE, destination, response);

            // then
            String streamKey = String.format(STREAM_KEY_FORMAT, TTL_TEST_JOIN_CODE);
            String idMapKey = String.format(ID_MAP_KEY_FORMAT, TTL_TEST_JOIN_CODE);

            Long streamKeyTtl = stringRedisTemplate.getExpire(streamKey);
            Long idMapKeyTtl = stringRedisTemplate.getExpire(idMapKey);

            // idMapKey는 짧은 TTL (2초 이하)
            assertThat(idMapKeyTtl).isGreaterThan(0L).isLessThanOrEqualTo(SHORT_DEDUP_TTL);

            // streamKey는 긴 TTL (5초 이하, idMapKey보다 큼)
            assertThat(streamKeyTtl).isGreaterThan(0L).isLessThanOrEqualTo(SHORT_STREAM_TTL)
                    .isGreaterThan(idMapKeyTtl);
        }
    }

    @Nested
    class 엣지_케이스 {

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        void 빈_문자열_데이터도_정상적으로_처리한다(String data) {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> response = WebSocketResponse.success(data);

            // when
            String streamId = wsRecoveryService.save(joinCode, destination, response);

            // then
            assertThat(streamId).isNotBlank();

            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");
            assertThat(messages).hasSize(1);
        }

        @Test
        void 특수문자가_포함된_데이터도_정상적으로_처리한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            String dataWithSpecialChars = "한글 데이터 🎮 <script>alert('xss')</script> \"quoted\"";
            WebSocketResponse<String> response = WebSocketResponse.success(dataWithSpecialChars);

            // when
            String streamId = wsRecoveryService.save(joinCode, destination, response);

            // then
            assertThat(streamId).isNotBlank();

            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");
            assertThat(messages).hasSize(1);
        }

        @Test
        void 대량의_메시지를_저장하고_복구한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            int largeMessageCount = 100;

            for (int i = 0; i < largeMessageCount; i++) {
                WebSocketResponse<String> response = WebSocketResponse.success("message" + i);
                wsRecoveryService.save(joinCode, destination, response);
            }

            // when
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            // maxLength 설정에 따라 일부만 유지될 수 있음
            assertThat(messages).isNotEmpty();
        }

        @Test
        void 에러_응답도_정상적으로_저장하고_복구한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            WebSocketResponse<String> errorResponse = WebSocketResponse.error("게임 시작 실패");

            // when
            String streamId = wsRecoveryService.save(joinCode, destination, errorResponse);
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");

            // then
            assertThat(streamId).isNotBlank();
            assertThat(messages).hasSize(1);
            assertThat(messages.getFirst().response().success()).isFalse();
            assertThat(messages.getFirst().response().errorMessage()).isEqualTo("게임 시작 실패");
        }

        @Test
        void 복잡한_객체_데이터도_정상적으로_처리한다() {
            // given
            String destination = "/topic/room/" + joinCode;
            List<String> complexData = List.of("player1", "player2", "player3");
            WebSocketResponse<List<String>> response = WebSocketResponse.success(complexData);

            // when
            String streamId = wsRecoveryService.save(joinCode, destination, response);

            // then
            assertThat(streamId).isNotBlank();
            List<RecoveryMessage> messages = wsRecoveryService.getMessagesSince(joinCode, "0-0");
            assertThat(messages).hasSize(1);
        }

        @Test
        void 서로_다른_방의_메시지는_독립적으로_관리된다() {
            // given
            String joinCode1 = "ABC3";
            String joinCode2 = "ABC4";
            cleanupRedis(joinCode1);
            cleanupRedis(joinCode2);

            String destination1 = "/topic/room/" + joinCode1;
            String destination2 = "/topic/room/" + joinCode2;

            WebSocketResponse<String> response1 = WebSocketResponse.success("room1 message");
            WebSocketResponse<String> response2 = WebSocketResponse.success("room2 message");

            wsRecoveryService.save(joinCode1, destination1, response1);
            wsRecoveryService.save(joinCode2, destination2, response2);

            // when
            wsRecoveryService.cleanup(joinCode1);

            // then
            List<RecoveryMessage> room1Messages = wsRecoveryService.getMessagesSince(joinCode1, "0-0");
            List<RecoveryMessage> room2Messages = wsRecoveryService.getMessagesSince(joinCode2, "0-0");

            assertThat(room1Messages).isEmpty();
            assertThat(room2Messages).hasSize(1);

            // cleanup
            cleanupRedis(joinCode2);
        }
    }

}
