package coffeeshout.profanity.fixture;

import coffeeshout.profanity.eval.GoldenItem;
import coffeeshout.profanity.eval.GoldenItem.Expected;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 골든셋 CSV와 평가 단위 테스트용 항목을 만든다.
 *
 * <p>CSV는 따옴표를 해석하지 않는다. 닉네임에 쉼표를 넣지 않는 것으로 파서를 대신한다.
 *
 * <p>세트는 둘이다. {@code golden}은 회귀 기준인 {@code golden.csv}, {@code hard}는 어려운 표본만 모은
 * {@code golden-hard.csv}다. 러너는 환경변수 {@code NICKNAME_GOLDEN_SET}으로 고른다.
 */
public final class NicknameGoldenSetFixture {

    public static final String 기본_세트 = "golden";
    public static final String 하드_세트 = "hard";

    private static final String 세트_환경변수 = "NICKNAME_GOLDEN_SET";
    private static final Map<String, String> 세트별_경로 =
            Map.of(기본_세트, "/nickname-audit/golden.csv", 하드_세트, "/nickname-audit/golden-hard.csv");

    private NicknameGoldenSetFixture() {}

    /** 환경변수가 고른 세트 이름. 비어 있으면 기본 세트다. */
    public static String 세트_이름() {
        return 세트_이름(System.getenv(세트_환경변수));
    }

    static String 세트_이름(String value) {
        return value == null || value.isBlank() ? 기본_세트 : value.trim();
    }

    public static List<GoldenItem> 골든셋() {
        return 골든셋(기본_세트);
    }

    public static List<GoldenItem> 골든셋(String 세트) {
        final String 경로 = 세트별_경로.get(세트);
        if (경로 == null) {
            throw new IllegalArgumentException("모르는 골든셋이다: " + 세트 + ". 쓸 수 있는 값은 " + 세트별_경로.keySet());
        }
        try (InputStream in = NicknameGoldenSetFixture.class.getResourceAsStream(경로);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(NicknameGoldenSetFixture::행)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static GoldenItem 욕설(String nickname, String category, String... terms) {
        return new GoldenItem(nickname, Expected.PROFANE, List.of(terms), category);
    }

    public static GoldenItem 우회(String nickname, String category, String... terms) {
        return new GoldenItem(nickname, Expected.EVASION, List.of(terms), category);
    }

    public static GoldenItem 경계(String nickname) {
        return new GoldenItem(nickname, Expected.AMBIGUOUS, List.of(), "경계");
    }

    public static GoldenItem 정상(String nickname, String category) {
        return new GoldenItem(nickname, Expected.CLEAN, List.of(), category);
    }

    private static GoldenItem 행(String line) {
        final String[] columns = line.split(",", -1);
        if (columns.length != 4) {
            throw new IllegalStateException("골든셋 행은 4열이어야 한다: " + line);
        }
        final List<String> terms = Arrays.stream(columns[2].split("\\|"))
                .filter(term -> !term.isBlank())
                .toList();
        return new GoldenItem(columns[0], Expected.valueOf(columns[1]), terms, columns[3]);
    }
}
