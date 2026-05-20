package coffeeshout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class UserModuleTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserModuleTestApplication.class, args);
    }
}
