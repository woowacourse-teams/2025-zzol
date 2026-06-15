package coffeeshout.profanity.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProfanityWordRepository {

    List<ProfanityWord> findAllActive();

    boolean existsByWord(String word);

    boolean save(ProfanityWord word);

    int bulkInsertIgnore(List<ProfanityWord> words);

    void deactivate(String word);

    void operatorAllow(String word, Language language);

    Optional<ProfanityWord> findByWord(String word);

    List<ProfanityWord> findAll();

    Page<ProfanityWord> findAllPaged(String search, Language language, WordSource source, Boolean activeOnly, Pageable pageable);

    void activate(String word);
}
