package coffeeshout.bombrelay.infra;

import coffeeshout.bombrelay.config.BombRelayDictionaryProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WordValidator {

    private final BombRelayDictionaryProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Boolean> cache = new ConcurrentHashMap<>();

    public WordValidator(BombRelayDictionaryProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public boolean isValidWord(String word) {
        final Boolean cached = cache.get(word);
        if (cached != null) {
            return cached;
        }

        try {
            final boolean result = queryDictionary(word);
            cache.put(word, result);
            return result;
        } catch (Exception e) {
            log.warn("사전 API 호출 실패, 단어 허용 처리: word={}", word, e);
            return true;
        }
    }

    private boolean queryDictionary(String word) throws Exception {
        final String encoded = URLEncoder.encode(word, StandardCharsets.UTF_8);
        final String url = String.format("%s?key=%s&q=%s&req_type=json&type1=word&pos=1",
                properties.apiUrl(), properties.apiKey(), encoded);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("사전 API 응답 오류: status={}, word={}", response.statusCode(), word);
            return response.statusCode() >= 500;
        }

        final JsonNode root = objectMapper.readTree(response.body());
        final JsonNode channel = root.path("channel");
        final int total = channel.path("total").asInt(0);

        if (total == 0) {
            return false;
        }

        return hasExactMatch(channel, word);
    }

    private boolean hasExactMatch(JsonNode channel, String word) {
        final JsonNode items = channel.path("item");
        if (items.isArray()) {
            for (JsonNode item : items) {
                if (word.equals(item.path("word").asText())) {
                    return true;
                }
            }
        } else if (items.isObject()) {
            return word.equals(items.path("word").asText());
        }
        return false;
    }
}
