package coffeeshout.admin.system.domain;

/**
 * 격리된 메시지가 어느 큐에서 왔는지.
 *
 * <p>둘을 한 화면에 나란히 두되 <b>합치지는 않는다.</b> 성격이 다르기 때문이다.
 *
 * <p>{@link #OUTBOX}는 아직 발행되지 못한 메시지다. 재시도 10번을 소진했을 뿐 내용은
 * 멀쩡할 수 있어서, 원인을 고친 뒤 다시 큐에 넣으면 정상 처리된다.
 *
 * <p>{@link #SETTLEMENT}는 소비 단계에서 격리된 메시지다. 그 테이블은 설계상
 * <b>재처리가 아니라 사후 분석</b>이 목적이라(SettlementDeadLetterEntity 주석) 다시
 * 넣는 경로를 두지 않았다. 정산은 중복 반영이 곧 잘못된 정산이라, 무엇이 이미 반영됐는지
 * 모르는 채로 다시 흘려보내는 것이 실패보다 나쁘다.
 */
public enum DeadLetterSource {

    /** outbox_event. 발행 실패. 다시 큐에 넣을 수 있다. */
    OUTBOX(true),

    /** settlement_dead_letter. 소비 실패. 보존과 분석용이라 재처리하지 않는다. */
    SETTLEMENT(false);

    private final boolean requeueable;

    DeadLetterSource(boolean requeueable) {
        this.requeueable = requeueable;
    }

    /** 화면이 버튼을 그릴지 말지를 이 값으로 정한다. 눌러 봐야 거절되는 버튼을 두지 않는다. */
    public boolean isRequeueable() {
        return requeueable;
    }
}
