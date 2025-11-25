package coffeeshout.racinggame.ui.response;

import coffeeshout.racinggame.domain.RacingRange;
import coffeeshout.racinggame.domain.RunnerPosition;
import java.util.List;

public record RacingGameRunnersStateResponse (RacingRange distance, List<RunnerPosition> players) {

}
