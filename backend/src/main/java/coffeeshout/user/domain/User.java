package coffeeshout.user.domain;

import java.util.Objects;

public class User {

    private final Long id;
    private final UserCode userCode;
    private UserNickname nickname;
    private final OAuthAccount oAuthAccount;

    public User(Long id, UserCode userCode, UserNickname nickname, OAuthAccount oAuthAccount) {
        Objects.requireNonNull(userCode, "userCode는 null일 수 없습니다.");
        Objects.requireNonNull(nickname, "nickname은 null일 수 없습니다.");
        Objects.requireNonNull(oAuthAccount, "oAuthAccount는 null일 수 없습니다.");
        this.id = id;
        this.userCode = userCode;
        this.nickname = nickname;
        this.oAuthAccount = oAuthAccount;
    }

    public UserNickname changeNickname(UserNickname newNickname) {
        this.nickname = newNickname;
        return this.nickname;
    }

    public Long getId() {
        return id;
    }

    public UserCode getUserCode() {
        return userCode;
    }

    public UserNickname getNickname() {
        return nickname;
    }

    public OAuthAccount getOAuthAccount() {
        return oAuthAccount;
    }
}
