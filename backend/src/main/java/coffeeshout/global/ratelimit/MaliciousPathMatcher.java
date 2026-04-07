package coffeeshout.global.ratelimit;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 악성 경로 패턴 감지기.
 * <p>
 * 정상 사용자가 접근할 가능성이 없는 경로를 정의한다.
 * 해당 경로 접근은 스캐너·봇으로 판단해 즉시 IP를 차단한다.
 */
@Component
public class MaliciousPathMatcher {

    private static final List<String> MALICIOUS_PREFIXES = List.of(
            "/.env",
            "/.git",
            "/.aws",
            "/.ssh",
            "/wp-admin",
            "/wp-login",
            "/wp-content",
            "/phpmyadmin",
            "/xmlrpc.php",
            "/cgi-bin",
            "/admin.php",
            "/config.php",
            "/setup.php",
            "/install.php",
            "/graphql"
    );

    public boolean isMalicious(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        String decodedPath = path;
        try {
            decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // 잘못된 URL 인코딩일 경우 원본 문자열을 그대로 사용합니다.
        }

        final String lowerPath = decodedPath.toLowerCase();
        return MALICIOUS_PREFIXES.stream().anyMatch(lowerPath::startsWith);
    }
}
