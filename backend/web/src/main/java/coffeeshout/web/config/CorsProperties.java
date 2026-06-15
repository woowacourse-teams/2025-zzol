package coffeeshout.web.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("web.cors")
@Getter
@Setter
public class CorsProperties {
    private List<String> allowedOrigins = new ArrayList<>();
}
