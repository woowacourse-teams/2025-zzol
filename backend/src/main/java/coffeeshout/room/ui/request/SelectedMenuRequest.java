package coffeeshout.room.ui.request;

import coffeeshout.room.domain.menu.MenuTemperature;

public record SelectedMenuRequest(Long id, String customName, MenuTemperature temperature) {

}
