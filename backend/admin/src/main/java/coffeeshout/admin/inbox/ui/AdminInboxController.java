package coffeeshout.admin.inbox.ui;

import coffeeshout.admin.inbox.application.InboxService;
import coffeeshout.admin.inbox.ui.response.InboxItemResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통합 작업함.
 *
 * <p>페이지 파라미터를 두지 않았다. 이 화면은 전부를 훑는 자리가 아니라 <b>다음에 처리할
 * 것</b>을 보는 자리다. 페이지를 넘기기 시작하면 그때부터는 각 화면의 목록이 할 일이고,
 * 여기서 흉내 내면 필터도 정렬도 없는 반쪽짜리 목록이 하나 더 생긴다.
 */
@RestController
@RequestMapping("/admin/api/inbox")
@RequiredArgsConstructor
public class AdminInboxController {

    private final InboxService inboxService;

    @GetMapping
    public List<InboxItemResponse> pending() {
        return inboxService.pending().stream().map(InboxItemResponse::from).toList();
    }
}
