package coffeeshout.profanity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = "coffeeshout")
@EnableJpaRepositories(basePackages = "coffeeshout")
@ConfigurationPropertiesScan(basePackages = "coffeeshout")
@SpringBootApplication(scanBasePackages = "coffeeshout")
public class ProfanityTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfanityTestApplication.class, args);
    }
}
