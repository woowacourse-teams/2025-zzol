package coffeeshout.room.ui.response;

import coffeeshout.room.domain.menu.Menu;

public record MenuResponse(
        Long id,
        String name,
        String categoryImageUrl
) {

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(menu.getId(), menu.getName(), menu.getCategoryImageUrl());
    }
}
