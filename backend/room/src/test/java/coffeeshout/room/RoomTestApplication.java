package coffeeshout.room;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// scanBasePackages는 @ComponentScan 범위만 지정하므로,
// JPA Repository/Entity 스캔과 ConfigurationProperties 스캔은 별도로 명시한다.
@EntityScan(basePackages = "coffeeshout")
@EnableJpaRepositories(basePackages = "coffeeshout")
@ConfigurationPropertiesScan(basePackages = {
        "coffeeshout.room",
        "coffeeshout.user",
        "coffeeshout.websocket",
        "coffeeshout.global",
        "coffeeshout.web"
})
@SpringBootApplication(scanBasePackages = {
        "coffeeshout.room",
        "coffeeshout.user",
        "coffeeshout.websocket",
        "coffeeshout.global",
        "coffeeshout.web"
})
public class RoomTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoomTestApplication.class, args);
    }
}
