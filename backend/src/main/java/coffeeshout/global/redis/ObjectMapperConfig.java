package coffeeshout.global.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ObjectMapperConfig {

    private static final String BASE_PACKAGE = "coffeeshout";

    @Bean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        registerBaseEventSubtypes(mapper);

        return mapper;
    }

    private void registerBaseEventSubtypes(ObjectMapper mapper) {
        final Reflections reflections = new Reflections(BASE_PACKAGE);
        final Set<Class<? extends BaseEvent>> eventTypes = reflections.getSubTypesOf(BaseEvent.class);

        for (Class<? extends BaseEvent> eventType : eventTypes) {
            mapper.registerSubtypes(eventType);
            log.debug("Registered event subtype: {}", eventType.getSimpleName());
        }

        log.info("Registered {} BaseEvent subtypes for Redis serialization", eventTypes.size());
    }
}
