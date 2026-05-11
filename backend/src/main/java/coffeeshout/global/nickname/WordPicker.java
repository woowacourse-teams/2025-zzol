package coffeeshout.global.nickname;

import java.util.List;

@FunctionalInterface
public interface WordPicker {

    String pick(List<String> words);
}
