package coffeeshout.admin.user.domain;

import java.time.Instant;

/**
 * 유저 검색 결과 한 줄.
 *
 * <p>이메일은 담지 않는다. {@code oauth_account.email}은 암호화 저장이라 복호화해야 읽히는데,
 * 백오피스가 그 열쇠를 쥘 이유가 없다. 사람을 특정하는 데는 유저코드로 충분하다.
 *
 * <p><b>탈퇴 여부도 담지 않는다.</b> {@code UserEntity}에 {@code @SQLRestriction("deleted_at IS NULL")}이
 * 걸려 있어 하이버네이트가 모든 조회에 그 조건을 덧붙인다. 즉 JPA 로 읽는 한 여기 오는 유저는
 * 전부 활성 회원이고, {@code deletedAt} 칸을 두면 언제나 null 이 찍힌다.
 * 늘 비어 있는 칸은 지표를 못 믿게 만든다.
 */
public record UserSummary(Long id, String userCode, String nickname, Instant createdAt) {}
