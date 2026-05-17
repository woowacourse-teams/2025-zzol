package coffeeshout.global.websocket.docs;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<WsCatalog> catalog() {
        final WsCatalog body = builder.build();
        final String etag = "\"" + Integer.toHexString(body.hashCode()) + "\"";
        return ResponseEntity.ok().eTag(etag).body(body);
    }
}
