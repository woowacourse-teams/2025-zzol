package coffeeshout.user.application.service;

import coffeeshout.global.nickname.RandomNameWordPool;
import coffeeshout.global.nickname.WordPicker;
import coffeeshout.user.domain.UserNickname;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NicknameDefaultGenerator {

    private final WordPicker wordPicker;

    public String generate() {
        final String adjective = wordPicker.pick(RandomNameWordPool.ADJECTIVES);
        final String noun = wordPicker.pick(RandomNameWordPool.NOUNS);
        final String candidate = adjective + noun;
        if (candidate.length() > UserNickname.MAX_LENGTH) {
            return noun;
        }
        return candidate;
    }
}
