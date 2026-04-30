package coffeeshout.user.domain;

public class User {

    private final Long id;
    private final UserCode userCode;
    private UserNickname nickname;
    private final OAuthAccount oAuthAccount;

    public User(Long id, UserCode userCode, UserNickname nickname, OAuthAccount oAuthAccount) {
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
