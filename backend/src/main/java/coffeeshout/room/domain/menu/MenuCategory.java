package coffeeshout.room.domain.menu;

import lombok.Getter;
import lombok.Setter;

@Getter
public class MenuCategory {

    @Setter
    private Long id;
    private final String name;
    private final String imageUrl;

    public MenuCategory(Long id, String name, String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
    }
}
