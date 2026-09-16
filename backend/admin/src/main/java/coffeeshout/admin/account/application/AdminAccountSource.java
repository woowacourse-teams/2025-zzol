package coffeeshout.admin.account.application;

public enum AdminAccountSource {

    /** 환경변수 ADMIN_EMAILS. UI로 삭제할 수 없다. */
    BOOTSTRAP,

    /** admin_account 테이블. UI로 추가/삭제한다. */
    DATABASE
}
