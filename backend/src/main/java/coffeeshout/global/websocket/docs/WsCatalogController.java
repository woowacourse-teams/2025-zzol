package coffeeshout.global.websocket.docs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!prod")
@ConditionalOnProperty(prefix = "websocket.docs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WsCatalogController {

    private final WsCatalogBuilder builder;

    public WsCatalogController(WsCatalogBuilder builder) {
        this.builder = builder;
    }

    @GetMapping("/dev/ws-catalog")
    public WsCatalog catalog() {
        return builder.build();
    }
}
