package coffeeshout.zzolbot.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * zzolbot이 외부 관측 백엔드(Loki·Prometheus·Tempo)를 호출할 때 쓰는 공통 HTTP 타임아웃.
 *
 * <p>타임아웃이 없으면 백엔드가 느리거나 응답하지 않을 때 호출 스레드가 무한정 점유된다.
 * 특히 모니터링 클라이언트({@code LokiLogClient})는
 * ToolExecutor의 도구 타임아웃 밖에서 직접 호출되므로 자체 타임아웃이 반드시 필요하다.
 */
public final class ZzolBotHttpTimeouts {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private ZzolBotHttpTimeouts() {}

    public static ClientHttpRequestFactory requestFactory() {
        return requestFactory(CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * 타임아웃을 직접 지정한다. 관측 백엔드와 응답 특성이 다른 호출에 쓴다.
     * 예를 들어 CPU 추론은 응답이 분 단위라 위 기본값 5초로는 매번 끊긴다.
     */
    public static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        final HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        final JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
