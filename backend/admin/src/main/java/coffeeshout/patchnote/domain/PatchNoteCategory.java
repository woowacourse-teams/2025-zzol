package coffeeshout.patchnote.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PatchNoteCategory {
    NOTICE("공지"),
    EVENT("이벤트"),
    UPDATE("업데이트"),
    MAINTENANCE("점검"),
    ;

    public final String label;
}
