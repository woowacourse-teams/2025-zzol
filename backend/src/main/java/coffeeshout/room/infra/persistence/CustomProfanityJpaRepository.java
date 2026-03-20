package coffeeshout.room.infra.persistence;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface CustomProfanityJpaRepository extends Repository<CustomProfanityEntity, Long> {

    CustomProfanityEntity save(CustomProfanityEntity entity);

    List<CustomProfanityEntity> findAll();

    boolean existsByWord(String word);
}
