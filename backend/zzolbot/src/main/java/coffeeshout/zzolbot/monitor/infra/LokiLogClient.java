package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.config.ZzolBotHttpTimeouts;
import coffeeshout.zzolbot.config.ZzolBotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Alertmanager 분석용 Loki 로그 클라이언트.
 * Alloy가 수집해 Loki에 적재한 로그에서 알림 시점의 ERROR 샘플을 뽑아 LLM 분석 근거로 제공한다.
 * Loki가 없거나(로컬) 조회 실패 시 빈 목록으로 안전하게 떨어진다.
 */
@Slf4j
@Component
public class LokiLogClient {

    private static final String LEVEL_ERROR = "ERROR";

    private final RestClient restClient;
    private final String lokiBaseUrl;
    private final String defaultEnvironment;
    private final ObjectMapper objectMapper;

    public LokiLogClient(ZzolBotProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.lokiBaseUrl = properties.monitoring().lokiUrl();
        this.restClient = restClientBuilder.baseUrl(lokiBaseUrl)
                .requestFactory(ZzolBotHttpTimeouts.requestFactory())
                .build();
        this.defaultEnvironment = properties.monitoring().environment();
        this.objectMapper = objectMapper;
    }

    /**
     * 알림에서 환경을 판별하지 못했을 때 쓸 폴백 환경(이 인스턴스가 실행 중인 환경).
     */
    public String defaultEnvironment() {
        return defaultEnvironment;
    }

    /**
     * {@code environment} 환경의 윈도우 구간 최근 ERROR 로그를 최대 {@code limit}건 반환(LLM 분석 근거).
     * <p>
     * 환경을 인자로 받는다. 이전에는 이 인스턴스의 설정값으로 고정해, dev에서 발화한 알림을 prod
     * 인스턴스가 받으면 prod 로그를 근거로 분석하는 불일치가 있었다(#1594).
     */
    public List<String> tailErrors(Instant end, Duration window, int limit, String environment) {
        final String logql = String.format("{environment=\"%s\"} |~ \"%s\"", environment, LEVEL_ERROR);
        final long startNano = end.minus(window).toEpochMilli() * 1_000_000L;
        final long endNano = end.toEpochMilli() * 1_000_000L;
        final URI uri = URI.create(lokiBaseUrl).resolve(String.format(
                "/loki/api/v1/query_range?query=%s&start=%d&end=%d&limit=%d&direction=backward",
                encode(logql), startNano, endNano, limit));
        try {
            final String body = restClient.get().uri(uri).retrieve().body(String.class);
            return parseMessages(body, limit);
        } catch (Exception e) {
            log.warn("[ZzolBot] Loki ERROR 로그 샘플 조회 실패 — 빈 목록", e);
            return List.of();
        }
    }

    private List<String> parseMessages(String raw, int limit) {
        final List<String> messages = new ArrayList<>();
        if (raw == null) {
            return messages;
        }
        try {
            final JsonNode results = objectMapper.readTree(raw).path("data").path("result");
            if (results.isArray()) {
                for (final JsonNode stream : results) {
                    for (final JsonNode entry : stream.path("values")) {
                        if (entry.isArray() && entry.size() >= 2 && messages.size() < limit) {
                            messages.add(entry.get(1).asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[ZzolBot] Loki 로그 응답 파싱 실패 — 빈 목록", e);
        }
        return messages;
    }

    private String encode(String value) {
        // URLEncoder는 공백을 '+'로 만든다. 인코딩 결과를 URI로 직접 넘기므로(RestClient 재인코딩 방지),
        // 공백은 어디서나 일관되게 해석되는 %20으로 통일한다.
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
