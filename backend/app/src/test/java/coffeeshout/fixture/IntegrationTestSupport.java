package coffeeshout.fixture;

import coffeeshout.support.TestContainerSupport;
import coffeeshout.support.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@IntegrationTest
public abstract class IntegrationTestSupport extends TestContainerSupport {

    @BeforeEach
    void cleanDatabaseBeforeEach() {
        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfterEach() {
        cleanDatabase();
    }
}
