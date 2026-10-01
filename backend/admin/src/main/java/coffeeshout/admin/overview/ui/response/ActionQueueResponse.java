package coffeeshout.admin.overview.ui.response;

import coffeeshout.admin.overview.application.OverviewService.ActionQueue;

/**
 * @param hasWork 하나라도 0이 아닌지. 화면이 값을 다시 더해 보지 않아도 되게 서버가 정한다.
 */
public record ActionQueueResponse(
        long pendingReports,
        long flaggedNicknames,
        long pendingNicknames,
        int blockedIps,
        long deadLetters,
        boolean hasWork) {

    public static ActionQueueResponse from(ActionQueue queue) {
        return new ActionQueueResponse(
                queue.pendingReports(),
                queue.flaggedNicknames(),
                queue.pendingNicknames(),
                queue.blockedIps(),
                queue.deadLetters(),
                queue.hasWork());
    }
}
