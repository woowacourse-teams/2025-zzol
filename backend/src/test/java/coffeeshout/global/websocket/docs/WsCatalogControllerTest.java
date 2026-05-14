package coffeeshout.global.websocket.docs;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@IntegrationTest
@DisplayName("WsCatalogController")
class WsCatalogControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("dev 환경에서 GET /dev/ws-catalog 가 200 으로 카탈로그를 반환한다")
    void 카탈로그를_반환한다() {
        final ResponseEntity<WsCatalog> response = restTemplate.getForEntity("/dev/ws-catalog", WsCatalog.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().stompEndpoint()).isEqualTo("/ws");
        assertThat(response.getBody().app()).isEqualTo("/app");
        assertThat(response.getBody().topicPrefix()).isEqualTo("/topic");
        assertThat(response.getBody().envelope().type()).isEqualTo("WebSocketResponse<T>");
    }
}
