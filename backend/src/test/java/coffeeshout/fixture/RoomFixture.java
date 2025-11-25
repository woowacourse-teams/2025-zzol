package coffeeshout.fixture;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.menu.SelectedMenu;
import coffeeshout.room.domain.player.Players;

public final class RoomFixture {

    private RoomFixture() {
    }

    public static Room 호스트_꾹이() {
        final Room room = new Room(
                new JoinCode("A4BX"),
                PlayerFixture.호스트꾹이().getName(),
                new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE)
        );
        final Players players = PlayersFixture.루키_엠제이_한스_리스트;
        players.getPlayers().forEach(player -> room.joinGuest(player.getName(), player.getSelectedMenu()));
        return room;
    }
}
