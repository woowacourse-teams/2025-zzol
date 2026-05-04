package coffeeshout.global.zzolbot.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.types.Schema;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ZzolBotSchemaConverterTest {

    private ZzolBotSchemaConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ZzolBotSchemaConverter();
    }

    @Nested
    class convert_메서드 {

        @Test
        void 빈_properties_스키마를_변환한다() {
            final Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", Map.of()
            );

            final Schema result = converter.convert(schema);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.type()).isPresent();
                softly.assertThat(result.type().get().toString()).containsIgnoringCase("object");
            });
        }

        @Test
        void string_타입_프로퍼티를_포함한_스키마를_변환한다() {
            final Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "joinCode", Map.of(
                                    "type", "string",
                                    "description", "4자리 방 입장 코드"
                            )
                    ),
                    "required", List.of("joinCode")
            );

            final Schema result = converter.convert(schema);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.properties()).isPresent();
                softly.assertThat(result.properties().get()).containsKey("joinCode");
                softly.assertThat(result.required()).isPresent();
                softly.assertThat(result.required().get()).containsExactly("joinCode");
            });
        }

        @Test
        void 복수_프로퍼티와_required를_포함한_스키마를_변환한다() {
            final Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "joinCode", Map.of("type", "string"),
                            "since", Map.of("type", "string", "description", "조회 기간")
                    ),
                    "required", List.of("joinCode")
            );

            final Schema result = converter.convert(schema);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.properties()).isPresent();
                softly.assertThat(result.properties().get()).hasSize(2);
                softly.assertThat(result.required()).isPresent();
                softly.assertThat(result.required().get()).containsExactly("joinCode");
            });
        }

        @Test
        void type과_description만_있는_단순_스키마를_변환한다() {
            final Map<String, Object> schema = Map.of(
                    "type", "string",
                    "description", "PromQL 표현식"
            );

            final Schema result = converter.convert(schema);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.description()).isPresent();
                softly.assertThat(result.description().get()).isEqualTo("PromQL 표현식");
                softly.assertThat(result.type()).isPresent();
                softly.assertThat(result.type().get().toString()).containsIgnoringCase("string");
            });
        }
    }
}
