package coffeeshout.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.context.ContextSnapshotFactory;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.ChannelInterceptor;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class ServiceTestConfig {

    @Bean("stompPrincipalInterceptor")
    @Primary
    public ChannelInterceptor mockStompPrincipalInterceptor() {
        ChannelInterceptor mock = Mockito.mock(ChannelInterceptor.class);
        Mockito.when(mock.preSend(Mockito.any(), Mockito.any()))
                .thenAnswer(inv -> inv.getArgument(0));
        return mock;
    }

    @Bean
    public ContextSnapshotFactory contextSnapshotFactory() {
        return ContextSnapshotFactory.builder().build();
    }

    @Bean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
