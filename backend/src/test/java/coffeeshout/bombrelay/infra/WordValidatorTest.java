package coffeeshout.bombrelay.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.bombrelay.config.BombRelayDictionaryProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WordValidatorTest {

    private HttpServer server;
    private WordValidator wordValidator;
    private String responseBody;
    private int responseStatus;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/search", exchange -> {
            final byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        final int port = server.getAddress().getPort();
        final BombRelayDictionaryProperties properties = new BombRelayDictionaryProperties(
                "http://localhost:" + port + "/api/search",
                "test-key"
        );
        wordValidator = new WordValidator(properties, new ObjectMapper());

        responseStatus = 200;
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Nested
    class 사전에_존재하는_단어 {

        @Test
        void 정확히_일치하는_단어가_있으면_true를_반환한다() {
            // given
            responseBody = """
                    {
                      "channel": {
                        "total": 1,
                        "item": [
                          { "word": "사과", "pos": "명사" }
                        ]
                      }
                    }
                    """;

            // when & then
            assertThat(wordValidator.isValidWord("사과")).isTrue();
        }

        @Test
        void 검색_결과에_여러_단어가_있고_정확히_일치하는_항목이_있으면_true를_반환한다() {
            // given
            responseBody = """
                    {
                      "channel": {
                        "total": 3,
                        "item": [
                          { "word": "사과나무", "pos": "명사" },
                          { "word": "사과", "pos": "명사" },
                          { "word": "사과하다", "pos": "동사" }
                        ]
                      }
                    }
                    """;

            // when & then
            assertThat(wordValidator.isValidWord("사과")).isTrue();
        }
    }

    @Nested
    class 사전에_존재하지_않는_단어 {

        @Test
        void total이_0이면_false를_반환한다() {
            // given
            responseBody = """
                    {
                      "channel": {
                        "total": 0
                      }
                    }
                    """;

            // when & then
            assertThat(wordValidator.isValidWord("ㅋㅋㅋ")).isFalse();
        }

        @Test
        void 검색_결과에_정확히_일치하는_단어가_없으면_false를_반환한다() {
            // given
            responseBody = """
                    {
                      "channel": {
                        "total": 1,
                        "item": [
                          { "word": "사과나무", "pos": "명사" }
                        ]
                      }
                    }
                    """;

            // when & then
            assertThat(wordValidator.isValidWord("사과")).isFalse();
        }
    }

    @Nested
    class API_오류_처리 {

        @Test
        void 응답_상태가_200이_아니면_단어를_허용한다() {
            // given
            responseStatus = 500;
            responseBody = "Internal Server Error";

            // when & then — 장애 시 게임 진행을 막지 않기 위해 true
            assertThat(wordValidator.isValidWord("사과")).isTrue();
        }
    }

    @Nested
    class 캐시 {

        @Test
        void 같은_단어를_두_번_검증하면_캐시에서_반환한다() {
            // given
            responseBody = """
                    {
                      "channel": {
                        "total": 1,
                        "item": [
                          { "word": "사과", "pos": "명사" }
                        ]
                      }
                    }
                    """;

            // when
            final boolean first = wordValidator.isValidWord("사과");

            // 서버 응답을 바꿔도 캐시된 결과를 반환해야 함
            responseBody = """
                    { "channel": { "total": 0 } }
                    """;
            final boolean second = wordValidator.isValidWord("사과");

            // then
            assertThat(first).isTrue();
            assertThat(second).isTrue();
        }
    }
}
