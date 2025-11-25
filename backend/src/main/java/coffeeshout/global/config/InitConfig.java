package coffeeshout.global.config;

import coffeeshout.room.application.DataInitializer;
import coffeeshout.room.application.MenuCategoryInitializer;
import coffeeshout.room.application.MenuInitializer;
import coffeeshout.room.domain.service.MenuCategoryCommandService;
import coffeeshout.room.domain.service.MenuCategoryQueryService;
import coffeeshout.room.domain.service.MenuCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class InitConfig {

    private final MenuCommandService menuCommandService;
    private final MenuCategoryCommandService menuCategoryCommandService;
    private final MenuCategoryQueryService menuCategoryQueryService;

    @Bean
    public MenuInitializer menuInitializer() {
        return new MenuInitializer(menuCommandService, menuCategoryQueryService);
    }

    @Bean
    public MenuCategoryInitializer menuCategoryInitializer() {
        return new MenuCategoryInitializer(menuCategoryCommandService);
    }

    @Bean
    public DataInitializer dataInitializer(MenuInitializer menuInitializer, MenuCategoryInitializer menuCategoryInitializer) {
        return new DataInitializer(menuInitializer, menuCategoryInitializer);
    }
}
