package coffeeshout.dashboard.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record TopWinnerResponse(@JsonUnwrapped RankedMember winner, Long winCount) {}
