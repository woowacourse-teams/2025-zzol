package coffeeshout.global.config.aws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Profile({"!local & !test"})
@Configuration
public class AwsConfig {

    @Bean
    public InstanceProfileCredentialsProvider instanceProfileCredentialsProvider() {
        return InstanceProfileCredentialsProvider.builder()
                .asyncCredentialUpdateEnabled(true)
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(DefaultAwsRegionProviderChain.builder().build().getRegion())
                .credentialsProvider(instanceProfileCredentialsProvider())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(DefaultAwsRegionProviderChain.builder().build().getRegion())
                .credentialsProvider(instanceProfileCredentialsProvider())
                .build();
    }
}
