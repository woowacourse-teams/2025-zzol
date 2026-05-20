package coffeeshout.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import coffeeshout.websocket.lifecycle.WebSocketGracefulShutdownHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@ExtendWith(MockitoExtension.class)
class GracefulShutdownHealthIndicatorTest {

    @Mock
    private WebSocketGracefulShutdownHandler shutdownHandler;

    private GracefulShutdownHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new GracefulShutdownHealthIndicator(shutdownHandler);
    }

    @Test
    void 정상_상태에서는_UP을_반환한다() {
        given(shutdownHandler.isShuttingDown()).willReturn(false);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void 종료_중에는_OUT_OF_SERVICE를_반환한다() {
        given(shutdownHandler.isShuttingDown()).willReturn(true);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("reason", "Graceful shutdown in progress");
    }
}
