package coffeeshout.fixture;

import coffeeshout.room.domain.audit.NicknameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditEntity;

public class NicknameAuditFixture {

    public static NicknameAuditEntity 검열대기(String nickname) {
        return new NicknameAuditEntity(nickname);
    }

    public static NicknameAuditEntity 검열완료_FLAGGED(String nickname) {
        NicknameAuditEntity entity = new NicknameAuditEntity(nickname);
        entity.complete(NicknameAuditStatus.FLAGGED, 0.92, "욕설 포함");
        return entity;
    }
}
