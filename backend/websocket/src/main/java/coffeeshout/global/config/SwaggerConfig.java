package coffeeshout.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Coffee Shout API")
                        .description("커피빵 API 문서")
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server().url("/").description("현재 서버")
                ));
    }
}
