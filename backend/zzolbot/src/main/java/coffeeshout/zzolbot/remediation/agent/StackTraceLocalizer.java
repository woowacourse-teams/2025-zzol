package coffeeshout.zzolbot.remediation.agent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Java 스택트레이스에서 결함 위치(file:line·모듈)를 LLM 없이 특정한다. 첫 {@code coffeeshout} 프레임을
 * 고르고(스로우 지점에 가장 가까운 앱 프레임), 그 클래스의 패키지·소스 파일을 repo에서 찾아 매핑한다.
 *
 * <p>Alloy 멀티라인 병합(Phase 0) 덕에 Loki에서 온전한 스택트레이스를 받을 수 있어야 이 단계가 성립한다.
 * 프레임을 못 찾으면 빈 결과를 반환하고, 호출측은 NO_FIX로 떨군다(틀린 파일을 고치지 않는다).
 */
public class StackTraceLocalizer {

    // 예: "at coffeeshout.room.application.RoomService.find(RoomService.java:42)"
    // group1 = FQN+메서드(코틀린 lambda$·Inner$ 포함), group2 = 소스 파일 base, group3 = 줄 번호.
    private static final Pattern FRAME = Pattern.compile(
            "at\\s+(coffeeshout[\\w.$]*)\\(([\\w$]+)\\.java:(\\d+)\\)");
    private static final String SRC_MAIN_JAVA = "src/main/java";
    private static final int MAX_WALK_DEPTH = 12;

    public Optional<DefectLocation> localize(String stackTrace, Path repoRoot) {
        if (stackTrace == null || stackTrace.isBlank()) {
            return Optional.empty();
        }
        final Matcher matcher = FRAME.matcher(stackTrace);
        if (!matcher.find()) {
            return Optional.empty();
        }
        final String fqnWithMethod = matcher.group(1);
        final String fileBase = matcher.group(2);
        final int line;
        try {
            // 정규식이 숫자임은 보장하나, 비정상적으로 긴 값은 int 범위를 넘겨 던질 수 있다.
            line = Integer.parseInt(matcher.group(3));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        final int lastDot = fqnWithMethod.lastIndexOf('.');
        if (lastDot < 0) {
            return Optional.empty();
        }
        final String classFqnWithInner = fqnWithMethod.substring(0, lastDot);
        final String methodName = fqnWithMethod.substring(lastDot + 1);
        final String packagePath = packagePath(classFqnWithInner);
        final String relativeClassPath = packagePath + "/" + fileBase + ".java";

        return findSourceFile(repoRoot, relativeClassPath)
                .map(absolute -> new DefectLocation(
                        stripInnerClass(classFqnWithInner),
                        methodName,
                        repoRoot.relativize(absolute).toString(),
                        line,
                        gradleModule(repoRoot, absolute)));
    }

    /**
     * 클래스 FQN(내부클래스 {@code $} 포함)에서 패키지 디렉터리 경로를 만든다.
     */
    private String packagePath(String classFqnWithInner) {
        final String topLevel = stripInnerClass(classFqnWithInner);
        final int lastDot = topLevel.lastIndexOf('.');
        final String packageName = lastDot < 0 ? "" : topLevel.substring(0, lastDot);
        return packageName.replace('.', '/');
    }

    private String stripInnerClass(String classFqn) {
        final int dollar = classFqn.indexOf('$');
        return dollar < 0 ? classFqn : classFqn.substring(0, dollar);
    }

    /**
     * repo 안에서 {@code src/main/java/<relativeClassPath>}로 끝나는 실제 소스 파일을 찾는다.
     */
    private Optional<Path> findSourceFile(Path repoRoot, String relativeClassPath) {
        final String suffix = SRC_MAIN_JAVA + "/" + relativeClassPath;
        try (Stream<Path> paths = Files.walk(repoRoot, MAX_WALK_DEPTH)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().replace('\\', '/').endsWith(suffix))
                    .findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException("소스 파일 탐색 실패: " + suffix, e);
        }
    }

    /**
     * 절대 경로에서 {@code /src/main/java} 직전 디렉터리명을 모듈로 보고 Gradle 경로(:module)로 만든다.
     */
    private String gradleModule(Path repoRoot, Path absolute) {
        final String normalized = repoRoot.relativize(absolute).toString().replace('\\', '/');
        final int idx = normalized.indexOf("/" + SRC_MAIN_JAVA + "/");
        if (idx < 0) {
            return "";
        }
        final String beforeSrc = normalized.substring(0, idx);
        final int slash = beforeSrc.lastIndexOf('/');
        final String module = slash < 0 ? beforeSrc : beforeSrc.substring(slash + 1);
        return ":" + module;
    }
}
