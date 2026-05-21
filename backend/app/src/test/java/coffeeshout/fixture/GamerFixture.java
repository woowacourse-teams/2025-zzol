package coffeeshout.fixture;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.room.domain.player.PlayerName;

public final class GamerFixture {

    private GamerFixture() {
    }

    public static Gamer 게스트한스() {
        return Gamer.guest(new PlayerName("한스"));
    }

    public static Gamer 게스트꾹이() {
        return Gamer.guest(new PlayerName("꾹이"));
    }

    public static Gamer 게스트루키() {
        return Gamer.guest(new PlayerName("루키"));
    }

    public static Gamer 게스트엠제이() {
        return Gamer.guest(new PlayerName("엠제이"));
    }

    public static Gamer 게스트유령() {
        return Gamer.guest(new PlayerName("유령"));
    }

    public static Gamer 게스트철수() {
        return Gamer.guest(new PlayerName("철수"));
    }

    public static Gamer 게스트영희() {
        return Gamer.guest(new PlayerName("영희"));
    }

    public static Gamer 로그인한스(long userId) {
        return Gamer.loggedIn(new PlayerName("한스"), userId);
    }

    public static Gamer 로그인꾹이(long userId) {
        return Gamer.loggedIn(new PlayerName("꾹이"), userId);
    }

    public static Gamer 로그인루키(long userId) {
        return Gamer.loggedIn(new PlayerName("루키"), userId);
    }

    public static Gamer 로그인엠제이(long userId) {
        return Gamer.loggedIn(new PlayerName("엠제이"), userId);
    }
}
