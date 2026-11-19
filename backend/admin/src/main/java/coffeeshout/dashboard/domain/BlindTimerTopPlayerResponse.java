package coffeeshout.dashboard.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record BlindTimerTopPlayerResponse(@JsonUnwrapped RankedPlayerName player, long bestErrorMillis) {}
