package coffeeshout.admin.audit.domain;

public enum AdminAuditResult {
    SUCCESS,

    /** 조치가 거부되거나 실패했다. 실패도 남긴다. 반복된 실패가 곧 신호다. */
    FAILURE
}
