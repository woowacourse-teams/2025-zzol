package coffeeshout.app.config;

import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.minigame.domain.MiniGameType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MiniGameFactoryConfig {

    @Bean
    public Map<MiniGameType, MiniGameFactory> miniGameFactoryMap(List<MiniGameFactory> factories) {
        final EnumMap<MiniGameType, MiniGameFactory> map = new EnumMap<>(MiniGameType.class);
        factories.forEach(factory -> map.put(factory.type(), factory));
        return map;
    }
}
