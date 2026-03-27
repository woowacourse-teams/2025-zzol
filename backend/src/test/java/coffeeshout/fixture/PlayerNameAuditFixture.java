package coffeeshout.fixture;

import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditEntity;

public class PlayerNameAuditFixture {

    public static PlayerNameAuditEntity 검열대기(String playerName) {
        return new PlayerNameAuditEntity(playerName);
    }

    public static PlayerNameAuditEntity 검열완료_FLAGGED(String playerName) {
        PlayerNameAuditEntity entity = new PlayerNameAuditEntity(playerName);
        entity.complete(PlayerNameAuditStatus.FLAGGED, 0.92, "욕설 포함");
        return entity;
    }
}
