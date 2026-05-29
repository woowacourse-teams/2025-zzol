package coffeeshout.room.infra;

import coffeeshout.room.config.OracleObjectStorageProperties;
import coffeeshout.room.config.QrProperties;
import com.oracle.bmc.objectstorage.ObjectStorage;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class OracleObjectStorageTestConfig {

    @Bean
    @Primary
    public OracleObjectStorageService oracleObjectStorageService(
            ObjectStorage objectStorage,
            OracleObjectStorageProperties oracleProperties,
            QrProperties qrProperties,
            MeterRegistry meterRegistry
    ) {
        return new OracleObjectStorageService(objectStorage, oracleProperties, qrProperties, meterRegistry);
    }
}
