package coffeeshout.minigame.infra.persistence.mapper;

import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameResultEntity;
import coffeeshout.room.infra.persistence.PlayerEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class MiniGameResultEntityMapper {

    @Mapping(target = "miniGamePlay", source = "miniGameEntity")
    @Mapping(target = "player", source = "playerEntity")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "miniGameType", ignore = true)
    public abstract MiniGameResultEntity toEntity(MiniGameEntity miniGameEntity, PlayerEntity playerEntity,
                                                   Integer rank, Long score);

    @AfterMapping
    protected void initAfterMapping(@MappingTarget MiniGameResultEntity entity) {
        entity.initAfterMapping();
    }
}
