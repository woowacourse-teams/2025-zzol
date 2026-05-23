package coffeeshout.room.application.port;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CustomProfanityRepository {

    int insertIgnore(String word, String source);

    void upsertOperatorManual(String word);

    int deleteAiAuditByWord(String word);

    boolean existsByWord(String word);

    Slice<String> findWords(Pageable pageable);

    void deleteByWord(String word);
}
