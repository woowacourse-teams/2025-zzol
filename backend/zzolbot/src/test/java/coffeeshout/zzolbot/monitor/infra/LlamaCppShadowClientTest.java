package coffeeshout.zzolbot.monitor.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * 응답을 읽는 경로를 실제 HTTP로 태운다.
 *
 * <p><b>{@code ShadowModelClient}를 목으로 대체하면 이 클래스의 버그를 영원히 못 잡는다.</b>
 * 실제로 그랬다. 단위 테스트는 전부 초록인데 배포하면 전건 실패했다. Spring Boot 4의 메시지
 * 컨버터가 Jackson 3라 Jackson 2 타입을 만들지 못한 것인데, 목을 쓰면 컨버터를 아예 안 탄다(#1813).
 *
 * <p>{@code MockRestServiceServer}로도 잡히지 않는다. 클라이언트가 타임아웃을 주려고 요청 팩토리를
 * 직접 지정하는데, 그 과정에서 목이 심어둔 팩토리가 덮어써진다. 그래서 진짜 서버를 띄운다.
 */
class LlamaCppShadowClientTest {

    private static final FiringAlert ALERT = new FiringAlert(
            "AppErrorLogSpike", "warning", "fp-1", "ERROR 급증", "임계 초과", Map.of("alertname", "AppErrorLogSpike"));
    private static final List<String> LOGS = List.of("2026-09-22 ERROR consumer lag 12000");

    private final AtomicReference<String> responseBody = new AtomicReference<>("{}");

    private HttpServer server;
    private LlamaCppShadowClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            final byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        final String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        final MonitorProperties properties = new MonitorProperties(
                true, 30, 240, new MonitorProperties.ShadowProperties(true, baseUrl, 2000, 5000, 700));
        client = new LlamaCppShadowClient(properties, RestClient.builder(), new MonitorAnalysisContract());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Nested
    class 응답_본문을_읽는다 {

        @Test
        void 정상_응답에서_모델이_쓴_내용을_꺼낸다() {
            responseBody.set("{\"choices\":[{\"message\":{\"content\":\"{\\\"summary\\\":\\\"요약\\\"}\"}}]}");

            assertThat(client.generate(ALERT, LOGS, "dev")).isEqualTo("{\"summary\":\"요약\"}");
        }

        @Test
        void 내용에_raw_제어문자가_있어도_읽는다() {
            // 모델은 로그 줄을 그대로 옮겨 적는다. 이스케이프하리라는 보장이 없다(#1811).
            responseBody.set("{\"choices\":[{\"message\":{\"content\":\"인용\tat com.foo.Bar\"}}]}");

            assertThat(client.generate(ALERT, LOGS, "dev")).contains("인용");
        }

        @Test
        void 예상과_다른_모양이면_빈_문자열을_준다() {
            // 형식 실패는 호출부가 "근거 없음"과 구분해 기록한다. 여기서 예외로 만들지 않는다.
            responseBody.set("{\"choices\":[]}");

            assertThat(client.generate(ALERT, LOGS, "dev")).isEmpty();
        }
    }
}
