package coffeeshout.dashboard.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record WormGameTopPlayerResponse(@JsonUnwrapped RankedPlayerName player, Long bestSurvivalMillis) {}
