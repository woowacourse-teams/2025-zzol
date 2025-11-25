package coffeeshout.room.ui;

import coffeeshout.room.ui.response.MenuCategoryResponse;
import coffeeshout.room.ui.response.SelectableMenuResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Menu Category", description = "메뉴 카테고리 관련 API")
public interface MenuCategoryApi {

    @Operation(summary = "전체 메뉴 카테고리 조회", description = "모든 메뉴 카테고리 목록을 조회합니다.")
    ResponseEntity<List<MenuCategoryResponse>> getAllMenuCategories();

    @Operation(summary = "카테고리별 메뉴 조회", description = "특정 카테고리에 속한 메뉴 목록을 조회합니다.")
    ResponseEntity<List<SelectableMenuResponse>> getMenusByCategory(
            @Parameter(description = "메뉴 카테고리 ID", required = true) Long categoryId
    );
}
