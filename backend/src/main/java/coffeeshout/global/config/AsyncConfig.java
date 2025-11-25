package coffeeshout.global.config;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final ContextSnapshotFactory snapshotFactory;

    @Bean(destroyMethod = "close")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "qrCodeTaskExecutor")
    public Executor qrCodeTaskExecutor(ExecutorService virtualThreadExecutor) {
        final TaskExecutorAdapter adapter = new TaskExecutorAdapter(virtualThreadExecutor);
        adapter.setTaskDecorator(runnable -> {
            ContextSnapshot snapshot = snapshotFactory.captureAll();
            return snapshot.wrap(runnable);
        });

        return adapter;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("비동기 작업 실패: method={}, params={}", method.getName(), params, ex);
    }
}
