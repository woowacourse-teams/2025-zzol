package coffeeshout.fixture;

import coffeeshout.room.domain.audit.AiConfidence;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditEntity;
import java.math.BigDecimal;

public final class PlayerNameAuditFixture {

    private PlayerNameAuditFixture() {
    }

    public static PlayerNameAuditEntity 검열대기(String playerName) {
        return new PlayerNameAuditEntity(playerName);
    }

    public static PlayerNameAuditEntity 검열완료_FLAGGED(String playerName) {
        PlayerNameAuditEntity entity = new PlayerNameAuditEntity(playerName);
        entity.complete(PlayerNameAuditStatus.FLAGGED, new AiConfidence(new BigDecimal("0.92")), "욕설 포함");
        return entity;
    }
}
