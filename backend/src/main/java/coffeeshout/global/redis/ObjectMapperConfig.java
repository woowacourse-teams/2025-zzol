package coffeeshout.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ObjectMapperConfig {

    @Bean(name = "redisObjectMapper")
    public ObjectMapper objectMapper() {
        final ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}
