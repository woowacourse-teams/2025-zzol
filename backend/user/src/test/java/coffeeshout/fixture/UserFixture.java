package coffeeshout.fixture;

import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.UserNickname;

public final class UserFixture {

    private UserFixture() {
    }

    public static User 회원_엠제이() {
        return new User(
                null,
                new UserCode("AB3CD"),
                new UserNickname("엠제이"),
                new OAuthAccount(OAuthProvider.GOOGLE, "google-uid-1", "mj@example.com")
        );
    }

    public static User 회원_루키() {
        return new User(
                null,
                new UserCode("XY4ZQ"),
                new UserNickname("루키"),
                new OAuthAccount(OAuthProvider.KAKAO, "kakao-uid-1", "rookie@example.com")
        );
    }

    public static User 저장된_회원(Long id, String nickname) {
        return new User(
                id,
                new UserCode("GH6KL"),
                new UserNickname(nickname),
                new OAuthAccount(OAuthProvider.GOOGLE, "google-uid-" + id, "user" + id + "@example.com")
        );
    }
}
