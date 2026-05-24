package coffeeshout.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestSupport extends TestContainerSupport {

    @BeforeEach
    void cleanDatabaseBeforeEach() {
        cleanDatabase();
    }
}
