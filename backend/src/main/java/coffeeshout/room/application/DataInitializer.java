package coffeeshout.room.application;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

public class DataInitializer {

    private final MenuInitializer menuInitializer;
    private final MenuCategoryInitializer menuCategoryInitializer;

    public DataInitializer(MenuInitializer menuInitializer, MenuCategoryInitializer menuCategoryInitializer) {
        this.menuInitializer = menuInitializer;
        this.menuCategoryInitializer = menuCategoryInitializer;
    }

    @PostConstruct
    public void init() throws IOException {
        menuCategoryInitializer.init();
        menuInitializer.init();
    }
}
