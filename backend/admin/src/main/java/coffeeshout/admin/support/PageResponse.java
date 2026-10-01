package coffeeshout.admin.support;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 백오피스 목록 응답의 공통 규격.
 *
 * <p>스프링 {@code Page}를 그대로 직렬화하지 않는다. 그 JSON 구조는 안정성이 보장되지 않아
 * 부트 버전이 올라가면 프론트가 조용히 깨진다. 화면이 실제로 쓰는 다섯 개만 고정한다.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
