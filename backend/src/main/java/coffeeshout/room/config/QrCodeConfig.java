package coffeeshout.room.config;

import com.google.zxing.qrcode.QRCodeWriter;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;

@Configuration
@RequiredArgsConstructor
public class QrCodeConfig {

    private final ContextSnapshotFactory snapshotFactory;

    @Bean
    public QRCodeWriter qrCodeWriter() {
        return new QRCodeWriter();
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
}
