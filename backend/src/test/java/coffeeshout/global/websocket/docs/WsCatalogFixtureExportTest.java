package coffeeshout.global.websocket.docs;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.IntegrationTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("WsCatalog 컨트랙트 fixture")
class WsCatalogFixtureExportTest extends IntegrationTestSupport {

    private static final Path FIXTURE_PATH = Path.of(
            "src", "test", "resources", "__fixtures__", "ws-catalog.json"
    );

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisabledIfSystemProperty(named = "updateFixture", matches = "true")
    void 카탈로그_응답이_fixture_와_일치한다() throws Exception {
        assertThat(FIXTURE_PATH)
                .withFailMessage("ws-catalog.json 픽스처가 없습니다. -DupdateFixture=true 로 먼저 생성하세요.")
                .exists();
        assertThat(fetchCatalogContent())
                .as("ws-catalog.json 이 최신 카탈로그와 다릅니다. -DupdateFixture=true 로 갱신하세요.")
                .isEqualTo(Files.readString(FIXTURE_PATH));
    }

    @Test
    @EnabledIfSystemProperty(named = "updateFixture", matches = "true")
    void 카탈로그_응답을_fixture_로_갱신한다() throws Exception {
        final String content = fetchCatalogContent();
        Files.createDirectories(FIXTURE_PATH.getParent());
        Files.writeString(FIXTURE_PATH, content);
    }

    private String fetchCatalogContent() throws Exception {
        final ResponseEntity<WsCatalog> response = restTemplate.getForEntity("/dev/ws-catalog", WsCatalog.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            softly.assertThat(response.getBody()).isNotNull();
        });

        final String json = objectMapper.copy()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(response.getBody());
        return json + System.lineSeparator();
    }
}
