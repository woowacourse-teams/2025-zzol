package coffeeshout.config;

import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.user.application.port.ReportAnonymizationPort;
import java.util.EnumMap;
import java.util.Map;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class ServiceTestConfig {

    @Bean
    @Primary
    public ReportAnonymizationPort mockReportAnonymizationPort() {
        return Mockito.mock(ReportAnonymizationPort.class);
    }

    @Bean
    @Primary
    public Map<MiniGameType, MiniGameFactory> miniGameFactoryMap() {
        Map<MiniGameType, MiniGameFactory> map = new EnumMap<>(MiniGameType.class);
        for (MiniGameType type : MiniGameType.values()) {
            MiniGameFactory factory = Mockito.mock(MiniGameFactory.class);
            Playable playable = Mockito.mock(Playable.class);
            Mockito.when(playable.getMiniGameType()).thenReturn(type);
            Mockito.when(factory.type()).thenReturn(type);
            Mockito.when(factory.create(Mockito.anyString())).thenReturn(playable);
            map.put(type, factory);
        }
        return map;
    }
}
