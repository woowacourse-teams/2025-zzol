package coffeeshout.dashboard.domain;

/** 당첨자 랭킹 쿼리가 돌려주는 원본 행이다. 닉네임은 검열 전 원문이라 응답에 그대로 싣지 않는다. */
public record MemberScore(String nickname, String userCode, long score) {}
