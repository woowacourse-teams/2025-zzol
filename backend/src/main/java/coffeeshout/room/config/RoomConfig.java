package coffeeshout.room.config;

import com.vane.badwordfiltering.BadWordFiltering;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoomConfig {

    @Bean
    public BadWordFiltering badWordFiltering() {
        return new BadWordFiltering();
    }
}
