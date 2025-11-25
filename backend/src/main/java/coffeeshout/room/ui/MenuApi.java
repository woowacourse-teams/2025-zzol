package coffeeshout.room.ui;

import coffeeshout.room.ui.response.MenuResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Menu", description = "메뉴 관련 API")
public interface MenuApi {

    @Operation(summary = "전체 메뉴 조회", description = "모든 메뉴 목록을 조회합니다.")
    ResponseEntity<List<MenuResponse>> getAllMenus();
}
