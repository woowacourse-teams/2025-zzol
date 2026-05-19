package coffeeshout.websocket.docs;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@Profile("!prod")
public class WsCatalogController {

    private final WsCatalogBuilder builder;

    public WsCatalogController(WsCatalogBuilder builder) {
        this.builder = builder;
    }

    @GetMapping("/dev/ws-catalog")
    public ResponseEntity<WsCatalog> catalog(WebRequest request) {
        final String etag = builder.getEtag();
        if (request.checkNotModified(etag)) {
            return null;
        }
        return ResponseEntity.ok().eTag(etag).body(builder.build());
    }
}
