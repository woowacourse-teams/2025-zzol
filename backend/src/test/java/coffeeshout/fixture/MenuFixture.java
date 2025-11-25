package coffeeshout.fixture;

import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.menu.ProvidedMenu;
import coffeeshout.room.domain.menu.TemperatureAvailability;

public class MenuFixture {

    public static Menu 아메리카노() {
        return new ProvidedMenu(
                1L,
                "아메리카노",
                MenuCategoryFixture.커피(),
                TemperatureAvailability.BOTH
        );
    }

    public static Menu 라떼() {
        return new ProvidedMenu(
                2L,
                "라떼",
                MenuCategoryFixture.커피(),
                TemperatureAvailability.BOTH
        );
    }

    public static Menu 아이스티() {
        return new ProvidedMenu(
                3L,
                "아이스티",
                MenuCategoryFixture.에이드(),
                TemperatureAvailability.BOTH
        );
    }
}
