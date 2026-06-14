package coffeeshout.global.nickname;

import java.util.Set;

public record NicknamesCollectedEvent(Set<String> nicknames) {
}
