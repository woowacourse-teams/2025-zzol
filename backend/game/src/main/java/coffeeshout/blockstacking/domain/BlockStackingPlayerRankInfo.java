package coffeeshout.blockstacking.domain;

import jakarta.annotation.Nullable;

/**
 * @param topX     마지막으로 쌓은 블록의 왼쪽 끝. 아직 한 층도 쌓지 않았으면 null
 * @param topWidth 마지막으로 쌓은 블록의 폭. 아직 한 층도 쌓지 않았으면 null
 */
public record BlockStackingPlayerRankInfo(
        String name,
        int floor,
        boolean failed,
        @Nullable Double topX,
        @Nullable Double topWidth) {}
