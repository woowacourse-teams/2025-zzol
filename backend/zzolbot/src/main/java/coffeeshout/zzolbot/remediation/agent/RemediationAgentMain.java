package coffeeshout.zzolbot.remediation.agent;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Actions 워커에서 실행되는 코딩 에이전트 CLI(Gradle JavaExec {@code :zzolbot:proposePatch}).
 * 순수하게 동작한다 — 입력(디스패치 페이로드 + repo) → 출력(JSON 제안). git/gh/gradle 호출은 하지 않으며,
 * 그 오케스트레이션(적용·테스트·PR)은 워크플로우가 담당한다.
 *
 * <p>인자: {@code <inputJson> <repoRoot> <outputJson>}.
 * <ul>
 *   <li>inputJson: repository_dispatch의 client_payload(defectType·rootCauseHypothesis·stackTrace 등)</li>
 *   <li>repoRoot: 체크아웃된 repo 루트(backend/ 를 포함)</li>
 *   <li>outputJson: 결과를 쓸 경로. status=PROPOSED 또는 NO_FIX</li>
 * </ul>
 * 결함 위치를 못 찾거나 대상 파일이 없으면 NO_FIX로 떨군다(틀린 파일을 고치지 않는다).
 */
public final class RemediationAgentMain {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String MODEL_ENV = "ZZOL_BOT_MODEL";
    private static final String API_KEY_ENV = "GEMINI_ZZOL_BOT_API_KEY";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern CLASS_PATTERN =
            Pattern.compile("(?m)^\\s*(?:public\\s+)?(?:final\\s+)?class\\s+(\\w+)");

    private RemediationAgentMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("사용법: proposePatch <inputJson> <repoRoot> <outputJson>");
        }
        final ObjectMapper objectMapper = new ObjectMapper();
        final Path inputPath = Path.of(args[0]);
        final Path repoRoot = Path.of(args[1]).toAbsolutePath().normalize();
        final Path outputPath = Path.of(args[2]);

        final JsonNode payload = objectMapper.readTree(Files.readString(inputPath));
        final DefectType defectType = parseDefectType(payload.path("defectType").asText("UNKNOWN"));
        final String rootCause = payload.path("rootCauseHypothesis").asText("");
        final String stackTrace = payload.path("stackTrace").asText("");

        final Optional<DefectLocation> location = new StackTraceLocalizer().localize(stackTrace, repoRoot);
        if (location.isEmpty()) {
            writeNoFix(objectMapper, outputPath, "스택트레이스에서 결함 위치를 특정하지 못했습니다.");
            return;
        }
        final DefectLocation loc = location.get();
        final Path targetFile = repoRoot.resolve(loc.filePath());
        if (!Files.isRegularFile(targetFile)) {
            writeNoFix(objectMapper, outputPath, "대상 소스 파일을 찾지 못했습니다: " + loc.filePath());
            return;
        }

        final String targetSource = Files.readString(targetFile);
        final DefectContext context = new DefectContext(defectType, rootCause, stackTrace, loc, targetSource);
        final CodingAgent agent = new GeminiCodingAgent(buildClient(), model(), objectMapper);
        final PatchProposal proposal = agent.propose(context);

        if (proposal.modifiedSource().isBlank() || proposal.reproTestSource().isBlank()
                || proposal.reproTestPath().isBlank()) {
            writeNoFix(objectMapper, outputPath, "에이전트가 적용 가능한 수정·재현 테스트를 만들지 못했습니다.");
            return;
        }
        final String[] normalizedTest = normalizeTestLocation(loc, proposal);
        writeProposed(objectMapper, outputPath, loc, proposal, normalizedTest[0], normalizedTest[1]);
        System.out.printf("[ZzolBot] 수정 제안 생성: %s (module=%s, test=%s)%n",
                loc.filePath(), loc.gradleModule(), normalizedTest[1]);
    }

    /**
     * LLM이 준 테스트 경로를 그대로 믿지 않고, 테스트 소스의 {@code package}/{@code class} 선언으로 경로·FQN을
     * 대상 모듈 기준으로 재구성한다. LLM이 {@code backend/} 접두를 빼거나 package와 경로가 어긋나도 테스트가
     * 올바른 위치에 놓이게 해, 정상 수정이 위치 불일치만으로 NO_FIX가 되는 것을 막는다. 파싱 실패 시 LLM 값으로 폴백.
     *
     * @return [reproTestPath, reproTestClass]
     */
    static String[] normalizeTestLocation(DefectLocation loc, PatchProposal proposal) {
        final String module = loc.gradleModule().replaceFirst("^:", "");
        final String packageName = match(PACKAGE_PATTERN, proposal.reproTestSource());
        final String className = match(CLASS_PATTERN, proposal.reproTestSource());
        if (module.isEmpty() || className.isEmpty()) {
            return new String[]{proposal.reproTestPath(), reproTestClass(proposal.reproTestPath())};
        }
        final String packageDir = packageName.isEmpty() ? "" : packageName.replace('.', '/') + "/";
        final String path = "backend/" + module + "/src/test/java/" + packageDir + className + ".java";
        final String fqn = packageName.isEmpty() ? className : packageName + "." + className;
        return new String[]{path, fqn};
    }

    private static String match(Pattern pattern, String source) {
        final Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static Client buildClient() {
        final String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(API_KEY_ENV + "가 설정되지 않았습니다.");
        }
        return Client.builder().apiKey(apiKey).build();
    }

    private static String model() {
        final String model = System.getenv(MODEL_ENV);
        return model == null || model.isBlank() ? DEFAULT_MODEL : model;
    }

    static DefectType parseDefectType(String raw) {
        try {
            return DefectType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return DefectType.UNKNOWN;
        }
    }

    private static void writeNoFix(ObjectMapper objectMapper, Path outputPath, String reason) throws Exception {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "NO_FIX");
        out.put("reason", reason);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), out);
        System.out.println("[ZzolBot] NO_FIX: " + reason);
    }

    private static void writeProposed(ObjectMapper objectMapper, Path outputPath, DefectLocation loc,
                                      PatchProposal proposal, String reproTestPath, String reproTestClass)
            throws Exception {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PROPOSED");
        out.put("targetPath", proposal.targetPath());
        out.put("modifiedSource", proposal.modifiedSource());
        out.put("reproTestPath", reproTestPath);
        out.put("reproTestSource", proposal.reproTestSource());
        out.put("reproTestClass", reproTestClass);
        out.put("rationale", proposal.rationale());
        out.put("gradleModule", loc.gradleModule());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), out);
    }

    /**
     * 테스트 파일 경로에서 클래스 FQN을 결정적으로 도출한다(워커가 {@code --tests <FQN>}로 RED→GREEN을 검증).
     * 예: {@code .../src/test/java/coffeeshout/game/FooTest.java} → {@code coffeeshout.game.FooTest}.
     */
    static String reproTestClass(String reproTestPath) {
        final String normalized = reproTestPath.replace('\\', '/');
        final String marker = "src/test/java/";
        final int idx = normalized.indexOf(marker);
        if (idx < 0 || !normalized.endsWith(".java")) {
            return "";
        }
        final String classPath = normalized.substring(idx + marker.length(), normalized.length() - ".java".length());
        return classPath.replace('/', '.');
    }
}
