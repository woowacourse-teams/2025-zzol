package coffeeshout.zzolbot.monitor.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * zzolbot 모니터링 알림 전용 Slack RestClient 배선.
 */
@Configuration
@EnableConfigurationProperties(ZzolBotSlackProperties.class)
public class ZzolBotSlackConfig {

    @Bean("zzolBotSlackRestClient")
    public RestClient zzolBotSlackRestClient(ZzolBotSlackProperties properties) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
