package coffeeshout.dashboard.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record SpeedTouchTopPlayerResponse(@JsonUnwrapped RankedPlayerName player, long bestTime) {}
