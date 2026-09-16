package coffeeshout.admin.system.domain;

import java.time.Instant;

/**
 * 격리된 메시지 한 건.
 *
 * <p>두 테이블의 컬럼이 달라서 화면용으로 한 모양에 맞춘다. 화면이 두 표를 따로 그리면
 * 운영자는 "지금 격리된 게 몇 건인가"에 답하려고 두 숫자를 머리로 더해야 한다.
 *
 * @param reference 사람이 원본을 되짚을 때 쓰는 값. outbox 는 스트림 키, 정산은 record_id 다.
 *                  둘 다 로그에서 검색할 수 있는 문자열이라는 점이 같다.
 * @param reason    왜 격리됐는지. outbox 는 이유를 저장하지 않아 재시도 횟수로 대신한다.
 * @param payload   원문. <b>이게 이 화면의 존재 이유다.</b> 적체 건수는 Grafana 가 이미
 *                  게이지로 보여주지만, 무엇이 왜 막혔는지는 원문을 봐야만 안다.
 */
public record DeadLetter(
        DeadLetterSource source,
        Long id,
        String reference,
        String reason,
        String payload,
        Integer retryCount,
        Instant createdAt) {}
