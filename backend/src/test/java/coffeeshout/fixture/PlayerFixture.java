package coffeeshout.fixture;

import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.menu.SelectedMenu;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;

public final class PlayerFixture {

    private PlayerFixture() {
    }

    public static Player 호스트한스() {
        return Player.createHost(new PlayerName("한스"), new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
    }

    public static Player 호스트꾹이() {
        return Player.createHost(new PlayerName("꾹이"), new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
    }

    public static Player 호스트루키() {
        return Player.createHost(new PlayerName("루키"), new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
    }

    public static Player 호스트엠제이() {
        return Player.createHost(new PlayerName("엠제이"), new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
    }

    public static Player 게스트한스() {
        final Player 한스 = Player.createGuest(
                new PlayerName("한스"),
                new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE)
        );
        한스.updateReadyState(true);
        return 한스;
    }

    public static Player 호스트유령() {
        final Player 유령 = Player.createHost(
                new PlayerName("유령"),
                new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE)
        );
        유령.updateReadyState(true);
        return 유령;
    }

    public static Player 게스트꾹이() {
        final Player 꾹이 = Player.createGuest(
                new PlayerName("꾹이"),
                new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE)
        );
        꾹이.updateReadyState(true);
        return 꾹이;
    }

    public static Player 게스트루키() {
        final Player 루키 = Player.createGuest(
                new PlayerName("루키"),
                new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE)
        );
        루키.updateReadyState(true);
        return 루키;
    }

    public static Player 게스트엠제이() {
        final Player 엠제이 = Player.createGuest(
                new PlayerName("엠제이"),
                new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE)
        );
        엠제이.updateReadyState(true);
        return 엠제이;
    }
}
