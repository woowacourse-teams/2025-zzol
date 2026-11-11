package coffeeshout.dashboard.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record RacingGameTopPlayerResponse(@JsonUnwrapped RankedPlayerName player, long bestTime) {}
