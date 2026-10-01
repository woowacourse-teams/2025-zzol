package coffeeshout.admin.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("PageResponse")
class PageResponseTest {

    @Test
    void 페이지_메타데이터를_그대로_옮긴다() {
        final PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 7);

        final PageResponse<String> response = PageResponse.of(page, value -> value);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.totalPages()).isEqualTo(4);
    }

    @Test
    void 내용을_매퍼로_변환한다() {
        final PageImpl<Integer> page = new PageImpl<>(List.of(1, 2, 3));

        assertThat(PageResponse.of(page, String::valueOf).content()).containsExactly("1", "2", "3");
    }

    @Test
    void 빈_페이지도_구조를_유지한다() {
        // 화면이 content 가 null 인 경우를 따로 다루지 않아도 되게 한다.
        final PageResponse<String> response = PageResponse.of(new PageImpl<>(List.<String>of()), value -> value);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }
}
