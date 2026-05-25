package coffeeshout.zzolbot.infra.sql;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.config.ZzolBotProperties.SqlProperties;
import coffeeshout.zzolbot.config.ZzolBotProperties.TableSchema;
import coffeeshout.zzolbot.domain.ZzolBotErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SqlQueryValidatorTest {

    private static final ZzolBotProperties PROPERTIES = new ZzolBotProperties(
            "key",
            "model",
            8,
            new ZzolBotProperties.MonitoringProperties("l", "t", "p", "test"),
            new ZzolBotProperties.DeterminismProperties(0.1, 0.1),
            60,
            10000L,
            new SqlProperties(
                    List.of(
                            new TableSchema("app_user",
                                    List.of("id", "nickname", "created_at", "deleted_at"),
                                    List.of("provider_user_id", "refresh_token"),
                                    "회원 정보"),
                            new TableSchema("room",
                                    List.of("id", "join_code", "room_state", "created_at"),
                                    List.of(),
                                    "방 정보")
                    ),
                    100,
                    3
            )
    );

    private SqlQueryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SqlQueryValidator(PROPERTIES);
    }

    @Nested
    class 유효한_SELECT {

        @Test
        void 단일_SELECT_문은_검증을_통과한다() {
            final String sql = "SELECT id, nickname FROM app_user";

            final String result = validator.validate(sql);

            assertThat(result).contains("LIMIT");
        }

        @Test
        void LIMIT_없으면_maxRows로_자동_추가된다() {
            final String sql = "SELECT id FROM app_user";

            final String result = validator.validate(sql);

            assertThat(result).endsWith("LIMIT 100");
        }

        @Test
        void LIMIT이_maxRows_이하면_그대로_유지된다() {
            final String sql = "SELECT id FROM app_user LIMIT 10";

            final String result = validator.validate(sql);

            assertThat(result).contains("LIMIT 10");
        }

        @Test
        void LIMIT이_maxRows_초과하면_maxRows로_치환된다() {
            final String sql = "SELECT id FROM app_user LIMIT 9999";

            final String result = validator.validate(sql);

            assertThat(result).contains("LIMIT 100");
            assertThat(result).doesNotContain("9999");
        }

        @Test
        void GROUP_BY와_집계함수를_포함한_쿼리가_통과한다() {
            final String sql = "SELECT room_state, COUNT(id) FROM room GROUP BY room_state LIMIT 50";

            final String result = validator.validate(sql);

            assertThat(result.toLowerCase()).contains("limit 50");
        }

        @Test
        void JOIN을_포함한_허용_테이블_쿼리가_통과한다() {
            final String sql = "SELECT u.id, u.nickname, r.join_code "
                    + "FROM app_user u JOIN room r ON u.id = r.id LIMIT 20";

            final String result = validator.validate(sql);

            assertThat(result).isNotNull();
        }

        @Test
        void 끝에_세미콜론이_있어도_LIMIT이_올바르게_추가된다() {
            final String sql = "SELECT id FROM app_user;";

            final String result = validator.validate(sql);

            assertThat(result).contains("LIMIT 100");
        }
    }

    @Nested
    class DDL_DML_거부 {

        @Test
        void INSERT_문은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("INSERT INTO app_user (nickname) VALUES ('test')"),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }

        @Test
        void UPDATE_문은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("UPDATE app_user SET nickname = 'x' WHERE id = 1"),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }

        @Test
        void DELETE_문은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("DELETE FROM app_user WHERE id = 1"),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }

        @Test
        void DROP_TABLE_문은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("DROP TABLE app_user"),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }

        @Test
        void TRUNCATE_문은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("TRUNCATE TABLE app_user"),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }
    }

    @Nested
    class stacked_statement_거부 {

        @Test
        void 세미콜론으로_구분된_복수_구문은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT id FROM app_user; DROP TABLE app_user"),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }

        @Test
        void SELECT_두개를_나란히_넣으면_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT id FROM app_user; SELECT id FROM room"),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }
    }

    @Nested
    class 허용되지_않은_테이블 {

        @Test
        void 화이트리스트에_없는_테이블은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT id FROM oauth_account LIMIT 10"),
                    ZzolBotErrorCode.SQL_TABLE_NOT_ALLOWED
            );
        }

        @Test
        void friendship_테이블은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT id FROM friendship LIMIT 10"),
                    ZzolBotErrorCode.SQL_TABLE_NOT_ALLOWED
            );
        }
    }

    @Nested
    class 와일드카드_거부 {

        @Test
        void SELECT_전체_와일드카드는_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT * FROM app_user LIMIT 10"),
                    ZzolBotErrorCode.SQL_WILDCARD_NOT_ALLOWED
            );
        }

        @Test
        void 테이블_별칭_와일드카드도_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT u.* FROM app_user u LIMIT 10"),
                    ZzolBotErrorCode.SQL_WILDCARD_NOT_ALLOWED
            );
        }
    }

    @Nested
    class PII_컬럼_차단 {

        @Test
        void app_user의_provider_user_id는_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT id, provider_user_id FROM app_user LIMIT 10"),
                    ZzolBotErrorCode.SQL_COLUMN_BLOCKED
            );
        }

        @Test
        void app_user의_refresh_token은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT id, refresh_token FROM app_user LIMIT 10"),
                    ZzolBotErrorCode.SQL_COLUMN_BLOCKED
            );
        }

        @Test
        void 테이블_명시_없이_blocked_컬럼_사용해도_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT provider_user_id FROM app_user LIMIT 10"),
                    ZzolBotErrorCode.SQL_COLUMN_BLOCKED
            );
        }

        @Test
        void 허용된_컬럼만_사용하면_통과한다() {
            final String sql = "SELECT id, nickname, created_at FROM app_user LIMIT 10";

            final String result = validator.validate(sql);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    class SELECT_INTO_와_FOR_UPDATE_거부 {

        @Test
        void FOR_UPDATE는_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELECT id FROM app_user FOR UPDATE"),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }
    }

    @Nested
    class SQL_파싱_실패 {

        @Test
        void 문법_오류_SQL은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate("SELEKT id FORM app_user"),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }

        @Test
        void 빈_문자열은_거부된다() {
            assertCoffeeShoutException(
                    () -> validator.validate(""),
                    ZzolBotErrorCode.INVALID_SQL
            );
        }
    }
}
