package coffeeshout.room.ui.response;

import coffeeshout.room.domain.menu.ProvidedMenu;
import java.util.List;

public record SelectableMenuResponse(Long id, String name, String temperatureAvailability) {

    public static SelectableMenuResponse from(ProvidedMenu menu) {
        return new SelectableMenuResponse(menu.getId(), menu.getName(), menu.getTemperatureAvailability().name());
    }

    public static List<SelectableMenuResponse> from(List<ProvidedMenu> menu) {
        return menu.stream()
                .map(SelectableMenuResponse::from)
                .toList();
    }
}
