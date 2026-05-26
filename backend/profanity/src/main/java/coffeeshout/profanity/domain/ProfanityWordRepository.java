package coffeeshout.profanity.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProfanityWordRepository {

    List<ProfanityWord> findAllActive();

    Set<String> findAllActiveIn(Set<String> candidates);

    boolean existsByWord(String word);

    boolean save(ProfanityWord word);

    void deactivate(String word);

    void operatorAllow(String word, Language language);

    Optional<ProfanityWord> findByWord(String word);

    List<ProfanityWord> findAll();
}
