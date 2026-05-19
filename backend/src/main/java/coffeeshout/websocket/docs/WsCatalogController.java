package coffeeshout.websocket.docs;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!prod")
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
