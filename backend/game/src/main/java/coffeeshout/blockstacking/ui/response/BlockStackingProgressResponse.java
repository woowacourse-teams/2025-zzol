package coffeeshout.blockstacking.ui.response;

import coffeeshout.blockstacking.domain.BlockStackingPlayerRankInfo;
import java.util.List;

public record BlockStackingProgressResponse(List<BlockStackingPlayerRankInfo> players) {
}
