package coffeeshout.global.trace;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

@Component
public class ObservationRegistryProvider {

    private static ObservationRegistry OBSERVATION_REGISTRY;

    public ObservationRegistryProvider(ObservationRegistry observationRegistry) {
        this.OBSERVATION_REGISTRY = observationRegistry;
    }

    public static ObservationRegistry getObservationRegistry() {
        return OBSERVATION_REGISTRY;
    }
}
