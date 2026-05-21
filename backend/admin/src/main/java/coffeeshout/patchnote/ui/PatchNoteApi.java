package coffeeshout.patchnote.ui;

import coffeeshout.patchnote.ui.response.PatchNoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "PatchNote", description = "패치노트 API")
public interface PatchNoteApi {

    @Operation(summary = "패치노트 전체 조회", description = "모든 패치노트를 최신순으로 반환합니다.")
    ResponseEntity<List<PatchNoteResponse>> findAll();

    @Operation(summary = "최신 패치노트 조회", description = "가장 최근 패치노트 1건을 반환합니다. 없으면 204를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "최신 패치노트 반환")
    @ApiResponse(responseCode = "204", description = "패치노트 없음")
    ResponseEntity<PatchNoteResponse> findLatest();
}
