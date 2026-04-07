package coffeeshout.global.filter;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.ratelimit.IpBlockStore;
import coffeeshout.global.ratelimit.MaliciousPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IP 기반 차단 필터.
 * <p>
 * 세 가지 차단 정책을 적용한다:
 * <ol>
 *   <li>이미 차단된 IP는 요청 초입에서 즉시 429 반환</li>
 *   <li>악성 경로(스캐너 패턴) 접근 시 해당 IP를 즉시 차단하고 429 반환</li>
 *   <li>404 응답이 누적되면 차단 (IpBlockStore 위임)</li>
 * </ol>
 *
 * <p>{@code HIGHEST_PRECEDENCE}로 등록해 Security 필터 체인보다 먼저 실행한다.
 * 차단된 요청은 Security 처리 비용 없이 즉시 반환된다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IpBlockFilter extends OncePerRequestFilter {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{0,4}(:[0-9a-fA-F]{0,4}){2,7}$"
    );

    private final IpBlockStore ipBlockStore;
    private final MaliciousPathMatcher maliciousPathMatcher;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String ip = getClientIp(request);

        if (!isValidIp(ip)) {
            log.debug("유효하지 않은 IP 형식, 필터 처리 건너뜀: ip={}", ip);
            filterChain.doFilter(request, response);
            return;
        }

        if (ipBlockStore.isBlocked(ip)) {
            log.warn("차단된 IP 접근 시도: ip={} uri={}", ip, request.getRequestURI());
            writeBlockedResponse(response);
            return;
        }

        if (maliciousPathMatcher.isMalicious(request.getRequestURI())) {
            log.warn("악성 경로 접근 감지 → IP 즉시 차단: ip={} uri={}", ip, request.getRequestURI());
            ipBlockStore.blockImmediately(ip);
            writeBlockedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);

        if (response.getStatus() == HttpStatus.NOT_FOUND.value()) {
            ipBlockStore.incrementNotFoundAndBlockIfExceeded(ip);
        }
    }

    private void writeBlockedResponse(HttpServletResponse response) throws IOException {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, GlobalErrorCode.IP_BLOCKED.getMessage());
        problemDetail.setProperty("errorCode", GlobalErrorCode.IP_BLOCKED.getCode());
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("exception", IpBlockFilter.class.getSimpleName());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problemDetail);
    }

    private String getClientIp(HttpServletRequest request) {
        return ClientIpExtractor.extract(request);
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();
    }
}
