package coffeeshout.support;

import coffeeshout.support.annotation.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;

@IntegrationTest
public abstract class IntegrationTestSupport extends TestContainerSupport {

    @BeforeEach
    void cleanDatabaseBeforeEach() {
        cleanDatabase();
    }
}
