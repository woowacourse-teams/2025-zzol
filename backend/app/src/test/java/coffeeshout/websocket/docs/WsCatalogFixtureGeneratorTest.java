package coffeeshout.websocket.docs;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.app.IntegrationTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("WsCatalog fixture 생성")
class WsCatalogFixtureGeneratorTest extends IntegrationTestSupport {

    private static final Path FIXTURE_PATH = Path.of(
            "src", "test", "resources", "__fixtures__", "ws-catalog.json"
    );

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @EnabledIfSystemProperty(named = "updateFixture", matches = "true")
    void 카탈로그_응답을_fixture_로_갱신한다() throws Exception {
        final String content = fetchCatalogContent();
        Files.createDirectories(FIXTURE_PATH.getParent());
        Files.writeString(FIXTURE_PATH, content);
    }

    private String fetchCatalogContent() throws Exception {
        final ResponseEntity<WsCatalog> response = restTemplate.getForEntity("/dev/ws-catalog", WsCatalog.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        final String json = objectMapper.copy()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(response.getBody());
        return json + "\n";
    }
}
