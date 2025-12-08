package coffeeshout.global.config.oracle;

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("!local & !test")
public class OracleCloudConfig {

    @Bean
    public InstancePrincipalsAuthenticationDetailsProvider authenticationDetailsProvider() {
        try {
            return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
        } catch (Exception e) {
            log.error("Failed to create Instance Principals authentication provider", e);
            throw new RuntimeException("Oracle Cloud 인증 설정 실패", e);
        }
    }

    @Bean
    public ObjectStorage objectStorageClient(
            InstancePrincipalsAuthenticationDetailsProvider authProvider
    ) {
        return ObjectStorageClient.builder()
                .build(authProvider);
    }
}
