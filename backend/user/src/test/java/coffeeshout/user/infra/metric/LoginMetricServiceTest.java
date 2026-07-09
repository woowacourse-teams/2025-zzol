package coffeeshout.user.infra.metric;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginMetricServiceTest {

    private MeterRegistry meterRegistry;
    private LoginMetricService loginMetricService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        loginMetricService = new LoginMetricService(meterRegistry);
    }

    @Test
    void 시작_카운트는_provider별로_누적된다() {
        loginMetricService.countStart("kakao");
        loginMetricService.countStart("kakao");
        loginMetricService.countStart("google");

        assertThat(count("login.start", "kakao")).isEqualTo(2.0);
        assertThat(count("login.start", "google")).isEqualTo(1.0);
    }

    @Test
    void 성공_카운트는_provider별로_누적된다() {
        loginMetricService.countSuccess("kakao");

        assertThat(count("login.success", "kakao")).isEqualTo(1.0);
    }

    @Test
    void 시작과_성공은_별개의_메트릭이다() {
        loginMetricService.countStart("kakao");

        assertThat(count("login.start", "kakao")).isEqualTo(1.0);
        assertThat(meterRegistry.find("login.success").tag("provider", "kakao").counter()).isNull();
    }

    private double count(String name, String provider) {
        Counter counter = meterRegistry.find(name).tag("provider", provider).counter();
        assertThat(counter).isNotNull();
        return counter.count();
    }
}
