package coffeeshout.profanity.domain;

import java.util.List;
import java.util.Optional;

public interface ProfanityWordRepository {

    List<ProfanityWord> findAllActive();

    boolean existsByWord(String word);

    void save(ProfanityWord word);

    void deactivate(String word);

    Optional<ProfanityWord> findByWord(String word);

    List<ProfanityWord> findAll();
}
