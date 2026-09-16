package coffeeshout.admin.auth.domain;

import coffeeshout.admin.account.domain.AdminEmail;

/**
 * 인증된 관리자. 컨트롤러가 {@code @AuthenticationPrincipal}로 받아 감사 로그의 실행자로 쓴다.
 *
 * <p>회원(User) 도메인과 무관하다. 관리자 로그인은 회원 레코드를 만들지 않는다.
 */
public record AdminPrincipal(AdminEmail email) {}
