package coffeeshout.admin.inbox.domain;

import java.time.Instant;

/**
 * 작업함 한 줄.
 *
 * <p>출처가 다른 세 가지를 한 모양으로 맞춘다. 신고와 닉네임과 격리 메시지는 담긴 정보가
 * 서로 다르지만, 운영자가 목록에서 묻는 것은 늘 같다. <b>무엇에 대한 일이고, 왜 올라왔고,
 * 언제 들어왔는가.</b> 그 셋만 남기고 나머지는 각 화면이 보여준다.
 *
 * @param id 각 출처 안에서의 식별자. 화면이 상세를 여는 주소에 쓴다. 출처마다 타입이
 *           달라(신고는 Long, 격리는 source+id) 문자열로 맞춘다
 */
public record InboxItem(InboxKind kind, String id, String title, String detail, Instant occurredAt) {}
