package coffeeshout.zzolbot.remediation.infra;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.zzolbot.config.ZzolBotHttpTimeouts;
import coffeeshout.zzolbot.remediation.config.RemediationProperties;
import coffeeshout.zzolbot.remediation.domain.RemediationErrorCode;
import coffeeshout.zzolbot.remediation.domain.RemediationRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * GitHub REST API의 repository_dispatch로 자동 수정 워크플로우를 트리거한다.
 * {@code POST /repos/{owner}/{repo}/dispatches} 에 {@code event_type=zzolbot-remediation}과 client_payload를 보낸다.
 *
 * <p>fine-grained PAT을 쓴다(GITHUB_TOKEN으로는 repository_dispatch를 보낼 수 없고, 또한 그 토큰으로 연
 * PR은 다른 워크플로우 CI를 트리거하지 못한다). 토큰 미설정 시 명확히 실패시킨다.
 */
@Slf4j
@Component
@Profile("!test")
public class RestGitHubDispatchClient implements GitHubDispatchClient {

    private static final String EVENT_TYPE = "zzolbot-remediation";

    private final RestClient restClient;
    private final RemediationProperties properties;

    public RestGitHubDispatchClient(RestClient.Builder restClientBuilder, RemediationProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.github.com")
                .requestFactory(ZzolBotHttpTimeouts.requestFactory())
                .build();
        this.properties = properties;
    }

    @Override
    public void dispatch(RemediationRequest request) {
        if (properties.githubToken() == null || properties.githubToken().isBlank()) {
            throw new InfrastructureException(RemediationErrorCode.GITHUB_TOKEN_MISSING,
                    "ZZOL_BOT_GH_DISPATCH_TOKEN이 설정되지 않아 자동 수정을 디스패치할 수 없습니다.");
        }
        try {
            restClient.post()
                    .uri("/repos/{owner}/{repo}/dispatches", properties.repoOwner(), properties.repoName())
                    .header("Authorization", "Bearer " + properties.githubToken())
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildBody(request))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new InfrastructureException(RemediationErrorCode.GITHUB_DISPATCH_FAILED,
                    "GitHub repository_dispatch 호출 실패: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildBody(RemediationRequest request) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", request.attemptId());
        payload.put("monitorRunId", request.monitorRunId());
        payload.put("fingerprint", request.fingerprint());
        payload.put("severity", request.severity());
        payload.put("defectType", request.defectType().name());
        payload.put("rootCauseHypothesis", request.rootCauseHypothesis());
        payload.put("suggestedActions", request.suggestedActions());
        payload.put("stackTrace", request.stackTrace());
        return Map.of("event_type", EVENT_TYPE, "client_payload", payload);
    }
}
