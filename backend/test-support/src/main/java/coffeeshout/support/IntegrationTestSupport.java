package coffeeshout.support;

import coffeeshout.support.annotation.IntegrationTest;
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
