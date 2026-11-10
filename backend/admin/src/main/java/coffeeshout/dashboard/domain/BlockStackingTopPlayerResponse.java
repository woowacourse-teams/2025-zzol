package coffeeshout.dashboard.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record BlockStackingTopPlayerResponse(@JsonUnwrapped RankedPlayerName player, Long maxFloor) {}
