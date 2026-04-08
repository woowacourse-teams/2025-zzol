package coffeeshout.room.infra.persistence.mapper;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.RoomEntity;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class PlayerEntityMapper {

    @Mapping(target = "playerName", expression = "java(player.getName().value())")
    @Mapping(target = "roomSession", source = "roomSession")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    public abstract PlayerEntity toEntity(Player player, RoomEntity roomSession);

    @AfterMapping
    protected void initCreatedAt(@MappingTarget PlayerEntity entity) {
        entity.initCreatedAt();
    }

    public List<PlayerEntity> toEntities(List<Player> players, RoomEntity roomSession) {
        return players.stream().map(p -> toEntity(p, roomSession)).toList();
    }
}
