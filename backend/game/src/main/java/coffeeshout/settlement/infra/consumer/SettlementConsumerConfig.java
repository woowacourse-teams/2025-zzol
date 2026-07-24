package coffeeshout.settlement.infra.consumer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 정산 컨슈머 그룹 실행 환경. 브로드캐스트 스트림들과 달리 정산 스트림은 설정 기반
 * 스레드풀 자동 등록 대상이 아니므로(listener-enabled: false) 전용 풀을 여기서 만든다.
 */
@Slf4j
@Configuration
public class SettlementConsumerConfig {

    /**
     * 폴링 태스크가 스레드 하나를 영구 점유하고(ADR-0022), 리스너는 폴링 스레드에서 인라인
     * 실행되므로 단일 스레드면 폴링과 처리가 직렬화된다. 같은 컨슈머가 받은 메시지가 도착
     * 순서대로 처리되어, 같은 회원의 결과가 뒤섞이지 않는다.
     */
    @Bean(name = "settlementConsumerExecutor")
    public ThreadPoolTaskExecutor settlementConsumerExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("settlement-consumer-");
        executor.initialize();
        return executor;
    }

    /**
     * 컨슈머 이름. blue/green 컨테이너는 서로 다른 호스트명(dev-app-blue 등)을 가지므로
     * 호스트명이 곧 인스턴스 식별자다. 어느 인스턴스가 처리·미ACK 중인지 XPENDING에서
     * 바로 읽히도록 사람이 알아볼 수 있는 이름을 쓴다.
     */
    @Bean(name = "settlementConsumerName")
    public String settlementConsumerName() {
        final String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            final String fallback = "consumer-" + UUID.randomUUID().toString().substring(0, 8);
            log.warn("호스트명을 확인할 수 없어 임의 컨슈머 이름을 사용합니다: {}", fallback);
            return fallback;
        }
    }
}
