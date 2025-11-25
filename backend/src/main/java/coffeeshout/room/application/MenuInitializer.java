package coffeeshout.room.application;

import coffeeshout.room.domain.menu.ProvidedMenu;
import coffeeshout.room.domain.menu.TemperatureAvailability;
import coffeeshout.room.domain.service.MenuCategoryQueryService;
import coffeeshout.room.domain.service.MenuCommandService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class MenuInitializer implements DataInitializable {

    private final MenuCommandService menuCommandService;
    private final MenuCategoryQueryService menuCategoryQueryService;

    public MenuInitializer(
            MenuCommandService menuCommandService,
            MenuCategoryQueryService menuCategoryQueryService
    ) {
        this.menuCommandService = menuCommandService;
        this.menuCategoryQueryService = menuCategoryQueryService;
    }

    public void init() throws IOException {
        final Yaml yaml = new Yaml();
        try (final InputStream inputStream = new ClassPathResource("data/menu-data.yml").getInputStream()) {
            final MenuDtos menuDtos = yaml.loadAs(inputStream, MenuDtos.class);
            menuDtos.getMenus().forEach(item -> menuCommandService.save(new ProvidedMenu(
                    item.getId(),
                    item.getName(),
                    menuCategoryQueryService.getById(item.getCategoryId()),
                    TemperatureAvailability.from(item.getTemperatureAvailability())
            )));
        }
    }

    @Setter
    @Getter
    protected static class MenuDtos {
        private List<MenuDto> menus;
    }

    @Setter
    @Getter
    protected static class MenuDto {
        private Long id;
        private String name;
        private Long categoryId;
        private String temperatureAvailability;
    }
}
