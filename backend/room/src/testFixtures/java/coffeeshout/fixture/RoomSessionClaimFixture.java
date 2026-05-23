package coffeeshout.fixture;

import coffeeshout.room.infra.auth.RoomSessionClaim;

public final class RoomSessionClaimFixture {

    private RoomSessionClaimFixture() {
    }

    public static RoomSessionClaim 로그인_플레이어() {
        return RoomSessionClaim.of("ABCD", "홍길동", 42L);
    }

    public static RoomSessionClaim 익명_플레이어() {
        return RoomSessionClaim.ofAnonymous("ABCD", "익명이");
    }
}
