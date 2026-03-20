package coffeeshout.global.filter;

import coffeeshout.global.metric.HttpMetricService;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpMetricFilter extends OncePerRequestFilter {

    private final HttpMetricService httpMetricService;

    public HttpMetricFilter(HttpMetricService httpMetricService) {
        this.httpMetricService = httpMetricService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        httpMetricService.incrementConcurrentRequests();

        try {
            filterChain.doFilter(request, response);

            // 비동기 요청인 경우 완료 콜백 등록
            if (request.isAsyncStarted()) {
                request.getAsyncContext().addListener(new AsyncListener() {
                    @Override
                    public void onComplete(AsyncEvent event) {
                        httpMetricService.decrementConcurrentRequests();
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) {
                        httpMetricService.decrementConcurrentRequests();
                    }

                    @Override
                    public void onError(AsyncEvent event) {
                        httpMetricService.decrementConcurrentRequests();
                    }

                    @Override
                    public void onStartAsync(AsyncEvent event) {
                        // 비동기 시작 시에는 카운터 조작하지 않음
                    }
                });
            } else {
                httpMetricService.decrementConcurrentRequests();
            }
        } catch (Exception e) {
            httpMetricService.decrementConcurrentRequests();
            throw e;
        }
    }
}