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

/**
 * 골든셋 CSV와 평가 단위 테스트용 항목을 만든다.
 *
 * <p>CSV는 따옴표를 해석하지 않는다. 닉네임에 쉼표를 넣지 않는 것으로 파서를 대신한다.
 */
public final class NicknameGoldenSetFixture {

    private static final String 골든셋_경로 = "/nickname-audit/golden.csv";

    private NicknameGoldenSetFixture() {}

    public static List<GoldenItem> 골든셋() {
        try (InputStream in = NicknameGoldenSetFixture.class.getResourceAsStream(골든셋_경로);
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
