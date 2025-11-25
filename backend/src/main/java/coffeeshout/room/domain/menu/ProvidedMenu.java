package coffeeshout.room.domain.menu;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ProvidedMenu extends Menu {

    @Setter
    private Long id;

    private final MenuCategory menuCategory;

    public ProvidedMenu(
            Long id,
            String name,
            MenuCategory menuCategory,
            TemperatureAvailability temperatureAvailability
    ) {
        super(name, temperatureAvailability);
        this.id = id;
        this.menuCategory = menuCategory;
    }

    @Override
    public String getCategoryImageUrl() {
        return menuCategory.getImageUrl();
    }

    @Override
    public Long getId() {
        return id;
    }
}
