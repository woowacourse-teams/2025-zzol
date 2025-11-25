package coffeeshout.room.domain.menu;

import lombok.Getter;

@Getter
public abstract class Menu {

    private final String name;
    private final TemperatureAvailability temperatureAvailability;

    protected Menu(String name, TemperatureAvailability temperatureAvailability) {
        this.name = name;
        this.temperatureAvailability = temperatureAvailability;
    }

    public abstract String getCategoryImageUrl();

    public abstract Long getId();
}
