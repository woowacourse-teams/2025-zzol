package coffeeshout.dashboard.domain;

public record BlockStackingTopPlayerResponse(String playerName, Long maxFloor, boolean masked) {

    public BlockStackingTopPlayerResponse(String playerName, Long maxFloor) {
        this(playerName, maxFloor, false);
    }

    public BlockStackingTopPlayerResponse mask() {
        return new BlockStackingTopPlayerResponse(MaskedNickname.TEXT, maxFloor, true);
    }
}
