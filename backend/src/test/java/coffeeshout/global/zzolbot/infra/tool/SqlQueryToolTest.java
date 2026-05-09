package coffeeshout.global.zzolbot.infra.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.global.zzolbot.domain.AskContext;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import coffeeshout.global.zzolbot.domain.ZzolBotErrorCode;
import coffeeshout.global.zzolbot.infra.sql.SqlQueryResult;
import coffeeshout.global.zzolbot.infra.sql.SqlQueryRunner;
import coffeeshout.global.zzolbot.infra.sql.SqlQueryValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SqlQueryToolTest {

    private static final AskContext CTX = AskContext.stamp("req-1", List.of(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @Mock
    private SqlQueryValidator validator;

    @Mock
    private SqlQueryRunner runner;

    private SqlQueryTool tool;

    @BeforeEach
    void setUp() {
        tool = new SqlQueryTool(validator, runner, new ObjectMapper());
    }

    @Nested
    class execute_메서드 {

        @Test
        void 유효한_SQL이면_실행_결과와_메타를_JSON으로_반환한다() {
            final String rawSql = "SELECT id FROM app_user";
            final String validSql = rawSql + " LIMIT 100";
            given(validator.validate(rawSql)).willReturn(validSql);
            given(runner.run(validSql)).willReturn(
                    new SqlQueryResult(List.of(Map.of("id", 1L), Map.of("id", 2L)), false));

            final ToolExecutionResult result = tool.execute(Map.of("sql", rawSql), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isTrue();
                softly.assertThat(result.toolName()).isEqualTo(SqlQueryTool.TOOL_NAME);
                softly.assertThat(result.content()).contains("rowCount");
                softly.assertThat(result.content()).contains("executedSql");
                softly.assertThat(result.content()).contains("rows");
            });
        }

        @Test
        void 결과가_잘린_경우_truncated_true를_포함한다() {
            given(validator.validate(any())).willReturn("SELECT id FROM app_user LIMIT 100");
            given(runner.run(any())).willReturn(new SqlQueryResult(List.of(), true));

            final ToolExecutionResult result = tool.execute(Map.of("sql", "SELECT id FROM app_user"), CTX);

            assertThat(result.content()).contains("\"truncated\":true");
        }

        @Test
        void sql_파라미터가_누락되면_실패_결과를_반환한다() {
            final ToolExecutionResult result = tool.execute(Map.of(), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isFalse();
                softly.assertThat(result.toolName()).isEqualTo(SqlQueryTool.TOOL_NAME);
            });
        }

        @Test
        void sql_파라미터가_문자열이_아니면_실패_결과를_반환한다() {
            final ToolExecutionResult result = tool.execute(Map.of("sql", 123), CTX);

            assertThat(result.success()).isFalse();
        }

        @Test
        void 검증_실패하면_실패_결과를_반환한다() {
            willThrow(new BusinessException(ZzolBotErrorCode.INVALID_SQL, "SELECT 문만 허용됩니다."))
                    .given(validator).validate(any());

            final ToolExecutionResult result = tool.execute(Map.of("sql", "DROP TABLE app_user"), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isFalse();
                softly.assertThat(result.content()).contains("SELECT 문만 허용됩니다.");
            });
        }

        @Test
        void 실행_중_예외가_발생하면_실패_결과를_반환한다() {
            given(validator.validate(any())).willReturn("SELECT id FROM app_user LIMIT 100");
            willThrow(new InfrastructureException(ZzolBotErrorCode.SQL_EXECUTION_FAILED, "타임아웃"))
                    .given(runner).run(any());

            final ToolExecutionResult result = tool.execute(Map.of("sql", "SELECT id FROM app_user"), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isFalse();
                softly.assertThat(result.content()).contains("타임아웃");
            });
        }
    }

    @Nested
    class 메타데이터 {

        @Test
        void 도구명이_sql_query다() {
            assertThat(tool.name()).isEqualTo("sql_query");
        }

        @Test
        void description이_비어있지_않다() {
            assertThat(tool.description()).isNotBlank();
        }

        @Test
        void parameterSchema에_sql_필드가_required로_포함된다() {
            final Map<String, Object> schema = tool.parameterSchema();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(schema).containsKey("properties");
                softly.assertThat(schema.get("required").toString()).contains("sql");
            });
        }
    }
}
