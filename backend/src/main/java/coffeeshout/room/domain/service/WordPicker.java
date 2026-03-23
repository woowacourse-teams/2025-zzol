package coffeeshout.room.domain.service;

import java.util.List;

@FunctionalInterface
public interface WordPicker {

    String pick(List<String> words);
}
