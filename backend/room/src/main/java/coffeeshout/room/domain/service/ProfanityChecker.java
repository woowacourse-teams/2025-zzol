package coffeeshout.room.domain.service;

import java.util.List;

public interface ProfanityChecker {

    boolean contains(String text);

    void addAll(List<String> words);

    void add(String word);

    void remove(String word);
}
