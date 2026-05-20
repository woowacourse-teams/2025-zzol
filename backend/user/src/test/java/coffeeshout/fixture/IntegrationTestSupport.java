package coffeeshout.fixture;

import coffeeshout.support.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;

@IntegrationTest
public abstract class IntegrationTestSupport extends TestContainerSupport {

    @BeforeEach
    void cleanDatabaseBeforeEach() {
        cleanDatabase();
    }
}
