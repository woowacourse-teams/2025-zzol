package coffeeshout.profanity.infra;

import coffeeshout.profanity.application.ProfanityFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 비속어 트라이 주기적 재빌드 스케줄러.
 *
 * <p>단어 변경 시 Redis pub/sub({@code profanity:trie:refresh})으로 즉시 동기화하지만(ADR-0018),
 * pub/sub은 비영속(fire-and-forget)이라 인스턴스가 구독 전이거나 일시 단절된 동안 발행된 신호는 유실된다.
 * 신호가 유실되면 해당 인스턴스의 인메모리 트라이는 재기동 전까지 DB와 영구히 불일치 상태로 남는다.
 *
 * <p>이 스케줄러는 그 유실을 자가 치유하는 <b>안전망</b>이다. 즉시성은 기존 pub/sub이 담당하고,
 * 본 스케줄러는 매시간 DB 기준으로 트라이를 재구성해 최대 1시간 내 정합성을 복구한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfanityTrieRebuildScheduler {

    private final ProfanityFilterService filterService;

    @Scheduled(cron = "0 0 * * * *")
    public void rebuildPeriodically() {
        log.debug("주기적 비속어 트라이 재빌드 시작 (pub/sub 신호 유실 대비 안전망)");
        try {
            filterService.rebuildTrie();
        } catch (Exception e) {
            log.error("주기적 비속어 트라이 재빌드 실패", e);
        }
    }
}
