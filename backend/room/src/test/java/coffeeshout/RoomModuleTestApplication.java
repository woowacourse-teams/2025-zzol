package coffeeshout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"coffeeshout.room", "coffeeshout.user", "coffeeshout.friend", "coffeeshout.profanity", "coffeeshout.global"})
@EnableJpaRepositories(basePackages = {"coffeeshout.room", "coffeeshout.user", "coffeeshout.friend", "coffeeshout.profanity", "coffeeshout.global"})
@ConfigurationPropertiesScan(basePackages = "coffeeshout")
@SpringBootApplication(scanBasePackages = "coffeeshout")
public class RoomModuleTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoomModuleTestApplication.class, args);
    }
}
