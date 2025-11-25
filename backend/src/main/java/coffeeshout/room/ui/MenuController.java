package coffeeshout.room.ui;

import coffeeshout.room.application.MenuService;
import coffeeshout.room.ui.response.MenuResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MenuController implements MenuApi {

    private final MenuService menuService;

    @GetMapping("/menus")
    public ResponseEntity<List<MenuResponse>> getAllMenus() {
        final List<MenuResponse> responses = menuService.getAll().stream()
                .map(MenuResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }
}
