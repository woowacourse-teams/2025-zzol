package coffeeshout.admin.system.ui;

import coffeeshout.admin.support.PageResponse;
import coffeeshout.admin.system.application.SystemService;
import coffeeshout.admin.system.domain.DeadLetterSource;
import coffeeshout.admin.system.ui.response.DeadLetterResponse;
import coffeeshout.admin.system.ui.response.DeploymentResponse;
import coffeeshout.admin.system.ui.response.MigrationsResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시스템 운영.
 *
 * <p>재처리와 폐기는 POST/DELETE 라 {@code AdminAuditAspect} 가 자동으로 감사 로그에
 * 남긴다. 격리된 정산 메시지를 누가 지웠는지는 나중에 반드시 묻게 되는 질문이다.
 */
@RestController
@RequestMapping("/admin/api/system")
@Validated
@RequiredArgsConstructor
public class AdminSystemController {

    private static final int PAGE_SIZE = 20;

    private final SystemService systemService;
    private final Environment environment;
    /**
     * {@code build-info.properties} 가 없으면 이 빈도 없다. 그래서 Optional 로 받는다.
     * 있으면 좋고 없어도 나머지 화면은 떠야 한다 - 버전 하나 때문에 기동이 막히면 안 된다.
     */
    private final Optional<BuildProperties> buildProperties;

    @GetMapping("/dead-letters")
    public PageResponse<DeadLetterResponse> deadLetters(
            @RequestParam(defaultValue = "OUTBOX") DeadLetterSource source,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(
                systemService.findDeadLetters(source, page, size == 0 ? PAGE_SIZE : size), DeadLetterResponse::from);
    }

    /** outbox 만 가능하다. 정산 DLQ 는 재처리가 아니라 보존이 목적이라 경로를 두지 않았다. */
    @PostMapping("/dead-letters/outbox/{id}/requeue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requeue(@PathVariable Long id) {
        systemService.requeue(id);
    }

    @DeleteMapping("/dead-letters/{source}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discard(@PathVariable DeadLetterSource source, @PathVariable Long id) {
        systemService.discard(source, id);
    }

    @GetMapping("/migrations")
    public MigrationsResponse migrations() {
        return MigrationsResponse.of(systemService.migrationHistoryExists(), systemService.migrations());
    }

    @GetMapping("/deployment")
    public DeploymentResponse deployment() {
        final String profile = String.join(",", environment.getActiveProfiles());
        return buildProperties
                .map(build -> new DeploymentResponse(build.getVersion(), build.get("commit"), build.getTime(), profile))
                .orElseGet(() -> new DeploymentResponse(null, null, null, profile));
    }
}
