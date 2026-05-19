package coffeeshout.global.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Graceful Shutdown 상태를 헬스체크에 반영한다.
 * <p>
 * 서버가 종료 중(shuttingDown=true)이면 OUT_OF_SERVICE를 반환하여
 * 로드밸런서가 이 인스턴스로 신규 트래픽을 보내지 않도록 한다.
 * <p>
 * DOWN이 아닌 OUT_OF_SERVICE를 사용하는 이유:
 * DOWN은 "장애"를 의미하고, Docker HEALTHCHECK에 의해 컨테이너가 재시작될 수 있다.
 * 종료 중인 서버를 재시작하면 종료 과정(세션 드레이닝)이 중단되므로,
 * "서비스 중단 중이지만 장애는 아님"을 표현하는 OUT_OF_SERVICE가 적합하다.
 */
@Component
@RequiredArgsConstructor
public class GracefulShutdownHealthIndicator implements HealthIndicator {

    private final ShutdownStateReader shutdownHandler;

    @Override
    public Health health() {
        if (shutdownHandler.isShuttingDown()) {
            return Health.outOfService()
                    .withDetail("reason", "Graceful shutdown in progress")
                    .build();
        }

        return Health.up().build();
    }
}
