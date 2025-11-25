package coffeeshout.room.domain.menu;

public enum MenuTemperature {
    HOT,
    ICE,
    ;

    public static MenuTemperature from(String temperature) {
        return valueOf(temperature);
    }
}
