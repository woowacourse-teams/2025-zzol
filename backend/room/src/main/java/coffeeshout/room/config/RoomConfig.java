package coffeeshout.room.config;

import coffeeshout.global.nickname.WordPicker;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoomConfig {

    @Bean
    public WordPicker wordPicker() {
        return words -> words.get(ThreadLocalRandom.current().nextInt(words.size()));
    }
}
