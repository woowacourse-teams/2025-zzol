package coffeeshout.global.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "oracle.cloud.objectstorage")
public record OracleObjectStorageProperties(
        @NotBlank String region,
        @NotBlank String namespace,
        @NotBlank String bucket
) {
}
