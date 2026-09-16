package coffeeshout.admin.inbox.ui.response;

import coffeeshout.admin.inbox.domain.InboxItem;
import coffeeshout.admin.inbox.domain.InboxKind;
import java.time.Instant;

/**
 * 작업함 한 줄의 응답.
 *
 * <p>도메인 레코드를 그대로 내보내지 않는다. 지금은 모양이 같지만, 화면이 한 칸을 더
 * 원할 때마다 도메인이 따라 바뀌면 도메인이 화면의 모양을 기억하게 된다.
 */
public record InboxItemResponse(InboxKind kind, String id, String title, String detail, Instant occurredAt) {

    public static InboxItemResponse from(InboxItem item) {
        return new InboxItemResponse(item.kind(), item.id(), item.title(), item.detail(), item.occurredAt());
    }
}
