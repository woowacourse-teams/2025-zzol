package coffeeshout.room.config;

import coffeeshout.nickname.WordPicker;
import com.vane.badwordfiltering.BadWordFiltering;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoomConfig {

    @Bean
    public BadWordFiltering badWordFiltering() {
        return new BadWordFiltering();
    }

    @Bean
    public WordPicker wordPicker() {
        return words -> words.get(ThreadLocalRandom.current().nextInt(words.size()));
    }
}
