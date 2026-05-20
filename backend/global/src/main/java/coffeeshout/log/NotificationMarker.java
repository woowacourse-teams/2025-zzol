package coffeeshout.log;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class NotificationMarker {

    public static final Marker INSTANCE = MarkerFactory.getMarker("[NOTIFICATION]");

    private NotificationMarker() {
    }
}
