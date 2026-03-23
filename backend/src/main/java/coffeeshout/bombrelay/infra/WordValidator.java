package coffeeshout.bombrelay.infra;

import coffeeshout.bombrelay.config.BombRelayDictionaryProperties;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Slf4j
@Component
public class WordValidator {

    private final BombRelayDictionaryProperties properties;
    private final HttpClient httpClient;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final ConcurrentMap<String, Boolean> cache = new ConcurrentHashMap<>();

    public WordValidator(BombRelayDictionaryProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.documentBuilderFactory = createSecureDocumentBuilderFactory();
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
        final String url = String.format("%s?key=%s&q=%s&type1=word&pos=1",
                properties.apiUrl(), properties.apiKey(), encoded);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .header("User-Agent", "CoffeeShout/1.0")
                .GET()
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("사전 API 응답 오류: status={}, word={}", response.statusCode(), word);
            return response.statusCode() >= 500;
        }

        final String body = response.body();
        if (body == null || body.isBlank()) {
            log.warn("사전 API 빈 응답, 단어 허용 처리: word={}", word);
            return true;
        }

        final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        final Document doc = builder.parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        final NodeList errorNodes = doc.getElementsByTagName("error_code");
        if (errorNodes.getLength() > 0) {
            final String errorCode = errorNodes.item(0).getTextContent();
            log.warn("사전 API 에러 응답: errorCode={}, word={}", errorCode, word);
            return true;
        }

        final NodeList totalNodes = doc.getElementsByTagName("total");
        if (totalNodes.getLength() == 0) {
            return false;
        }

        final int total = Integer.parseInt(totalNodes.item(0).getTextContent().trim());
        if (total == 0) {
            return false;
        }

        return hasExactMatch(doc, word);
    }

    private boolean hasExactMatch(Document doc, String word) {
        final NodeList items = doc.getElementsByTagName("item");
        return IntStream.range(0, items.getLength())
                .mapToObj(i -> (Element) items.item(i))
                .map(item -> item.getElementsByTagName("word"))
                .filter(wordNodes -> wordNodes.getLength() > 0)
                .map(wordNodes -> wordNodes.item(0).getTextContent().trim())
                .anyMatch(word::equals);
    }

    private static DocumentBuilderFactory createSecureDocumentBuilderFactory() {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception e) {
            log.warn("XXE 방어 설정 실패", e);
        }
        return factory;
    }
}
