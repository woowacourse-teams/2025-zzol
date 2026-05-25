package coffeeshout.room.config;

import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.user.application.port.ReportAnonymizationPort;
import java.util.EnumMap;
import java.util.Map;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RoomTestConfig {

    // 구현체가 :admin 모듈에 있으므로 room 테스트 컨텍스트에서는 mock으로 대체한다
    @Bean
    @Primary
    public ReportAnonymizationPort reportAnonymizationPort() {
        return Mockito.mock(ReportAnonymizationPort.class);
    }

    // MiniGameFactoryConfig은 :app 모듈에 있어 room 테스트 컨텍스트에서 로드되지 않는다
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
