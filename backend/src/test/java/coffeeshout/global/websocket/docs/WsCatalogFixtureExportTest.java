package coffeeshout.global.websocket.docs;

import coffeeshout.fixture.TestContainerSupport;
import coffeeshout.support.test.IntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@IntegrationTest
@DisplayName("WsCatalog 컨트랙트 fixture export")
class WsCatalogFixtureExportTest extends TestContainerSupport {

    private static final Path FIXTURE_PATH = Path.of(
            "src", "test", "resources", "__fixtures__", "ws-catalog.json"
    );

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("dev 카탈로그 응답을 tools/ws-mcp 가 zod 로 검증할 수 있도록 fixture 파일로 저장한다")
    void 카탈로그_응답을_fixture_로_저장한다() throws Exception {
        final ResponseEntity<WsCatalog> response = restTemplate.getForEntity("/dev/ws-catalog", WsCatalog.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            softly.assertThat(response.getBody()).isNotNull();
        });

        final String json = objectMapper.copy()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(response.getBody());

        Files.createDirectories(FIXTURE_PATH.getParent());
        Files.writeString(FIXTURE_PATH, json + System.lineSeparator());
    }
}
