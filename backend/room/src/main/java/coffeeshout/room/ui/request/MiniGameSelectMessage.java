package coffeeshout.room.ui.request;

import java.util.List;

public record MiniGameSelectMessage(
        String hostName,
        List<String> miniGameTypes
) {
}
