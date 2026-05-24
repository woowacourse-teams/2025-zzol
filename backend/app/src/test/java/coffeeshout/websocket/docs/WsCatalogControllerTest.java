package coffeeshout.websocket.docs;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.IntegrationTestSupport;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("WsCatalogController")
class WsCatalogControllerTest extends IntegrationTestSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    @Nested
    @DisplayName("dev 환경")
    class dev_환경 {

        @Test
        @DisplayName("GET /dev/ws-catalog 가 200 으로 카탈로그를 반환한다")
        void 카탈로그를_반환한다() {
            final ResponseEntity<WsCatalog> response = restTemplate.getForEntity("/dev/ws-catalog", WsCatalog.class);
            final WsCatalog body = response.getBody();
            assertThat(body).isNotNull();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                softly.assertThat(body.stompEndpoint()).isEqualTo("/ws");
                softly.assertThat(body.app()).isEqualTo("/app");
                softly.assertThat(body.topicPrefix()).isEqualTo("/topic");
                softly.assertThat(body.envelope().type()).isEqualTo("WebSocketResponse<T>");
            });
        }
    }

    @Nested
    @DisplayName("prod 가드")
    class prod_가드 {

        @Test
        @DisplayName("WsCatalogController 는 @Profile(!prod) 로 운영 환경 노출을 방지한다")
        void prod_프로파일_가드가_선언되어_있다() {
            final Profile profile = WsCatalogController.class.getAnnotation(Profile.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(profile).isNotNull();
                softly.assertThat(profile.value()).containsExactly("!prod");
            });
        }
    }
}
