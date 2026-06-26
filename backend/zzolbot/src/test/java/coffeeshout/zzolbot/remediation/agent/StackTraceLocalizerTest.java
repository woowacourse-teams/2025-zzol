package coffeeshout.zzolbot.remediation.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StackTraceLocalizerTest {

    private final StackTraceLocalizer localizer = new StackTraceLocalizer();

    private Path createSource(Path repoRoot, String module, String packagePath, String fileName) throws IOException {
        final Path dir = repoRoot.resolve("backend/" + module + "/src/main/java/" + packagePath);
        Files.createDirectories(dir);
        final Path file = dir.resolve(fileName);
        Files.writeString(file, "package x; class C {}");
        return file;
    }

    @Nested
    class 정상_특정 {

        @Test
        void 첫_coffeeshout_프레임으로_파일과_모듈을_특정한다(@TempDir Path repoRoot) throws IOException {
            createSource(repoRoot, "room", "coffeeshout/room/application", "RoomService.java");
            final String stackTrace = """
                    java.lang.NullPointerException: Cannot invoke method
                    \tat java.base/java.util.Optional.orElseThrow(Optional.java:403)
                    \tat coffeeshout.room.application.RoomService.find(RoomService.java:42)
                    \tat coffeeshout.room.ui.RoomController.get(RoomController.java:21)""";

            final Optional<DefectLocation> result = localizer.localize(stackTrace, repoRoot);

            assertThat(result).isPresent();
            SoftAssertions.assertSoftly(softly -> {
                final DefectLocation loc = result.get();
                softly.assertThat(loc.classFqn()).isEqualTo("coffeeshout.room.application.RoomService");
                softly.assertThat(loc.methodName()).isEqualTo("find");
                softly.assertThat(loc.lineNumber()).isEqualTo(42);
                softly.assertThat(loc.gradleModule()).isEqualTo(":room");
                softly.assertThat(loc.filePath())
                        .isEqualTo("backend/room/src/main/java/coffeeshout/room/application/RoomService.java");
            });
        }

        @Test
        void 내부클래스_프레임은_최상위_소스파일로_매핑한다(@TempDir Path repoRoot) throws IOException {
            createSource(repoRoot, "game", "coffeeshout/game/application", "GameFlow.java");
            final String stackTrace = "java.lang.NullPointerException\n"
                    + "\tat coffeeshout.game.application.GameFlow$Step.run(GameFlow.java:88)";

            final Optional<DefectLocation> result = localizer.localize(stackTrace, repoRoot);

            assertThat(result).isPresent();
            assertThat(result.get().filePath())
                    .isEqualTo("backend/game/src/main/java/coffeeshout/game/application/GameFlow.java");
        }
    }

    @Nested
    class 특정_실패 {

        @Test
        void coffeeshout_프레임이_없으면_빈값(@TempDir Path repoRoot) {
            final String stackTrace = "java.lang.NullPointerException\n"
                    + "\tat java.base/java.util.Optional.orElseThrow(Optional.java:403)";

            assertThat(localizer.localize(stackTrace, repoRoot)).isEmpty();
        }

        @Test
        void 소스파일이_repo에_없으면_빈값(@TempDir Path repoRoot) {
            final String stackTrace = "\tat coffeeshout.room.application.Missing.find(Missing.java:1)";

            assertThat(localizer.localize(stackTrace, repoRoot)).isEmpty();
        }

        @Test
        void null이거나_빈_스택트레이스는_빈값(@TempDir Path repoRoot) {
            assertThat(localizer.localize(null, repoRoot)).isEmpty();
            assertThat(localizer.localize("  ", repoRoot)).isEmpty();
        }
    }
}
