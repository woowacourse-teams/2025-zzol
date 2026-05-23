package coffeeshout.room.application.port;

import coffeeshout.room.infra.persistence.RouletteResultEntity;

public interface RouletteResultEntityRepository {

    RouletteResultEntity save(RouletteResultEntity entity);
}
