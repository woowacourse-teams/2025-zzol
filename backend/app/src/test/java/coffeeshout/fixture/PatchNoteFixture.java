package coffeeshout.fixture;

import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.infra.persistence.PatchNoteEntity;

public final class PatchNoteFixture {

    private PatchNoteFixture() {
    }

    public static PatchNoteEntity 공지_패치노트() {
        return PatchNoteEntity.create(PatchNoteCategory.NOTICE, "1.0.0 공지사항", "서버 점검이 예정되어 있습니다.");
    }

    public static PatchNoteEntity 업데이트_패치노트() {
        return PatchNoteEntity.create(PatchNoteCategory.UPDATE, "1.1.0 업데이트", "새로운 미니게임이 추가되었습니다.");
    }
}
