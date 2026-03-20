package coffeeshout.room.infra.persistence.nickname;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface CustomProfanityJpaRepository extends Repository<CustomProfanityEntity, Long> {

    CustomProfanityEntity save(CustomProfanityEntity entity);

    @Query("SELECT c.word FROM CustomProfanityEntity c ORDER BY c.id ASC")
    List<String> findWords(Pageable pageable);

    boolean existsByWord(String word);
}
