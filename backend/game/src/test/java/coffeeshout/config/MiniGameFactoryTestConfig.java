package coffeeshout.config;

import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.minigame.domain.MiniGameType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 프로덕션에선 {@code :app}의 {@code MiniGameFactoryConfig}가 제공하는 {@code Map<MiniGameType, MiniGameFactory>}를
 * {@code :game} 모듈 단위 테스트 컨텍스트에서 동일하게 조립한다. {@code :app}은 클래스패스 밖이므로
 * 실제 팩토리 빈({@code List<MiniGameFactory>})을 수집해 enum 키 맵으로 만든다.
 * {@code GameSessionService.updateGames}가 실 게임 인스턴스를 생성하도록 mock이 아닌 실구현 팩토리를 사용한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MiniGameFactoryTestConfig {

    @Bean
    public Map<MiniGameType, MiniGameFactory> miniGameFactoryMap(List<MiniGameFactory> factories) {
        final EnumMap<MiniGameType, MiniGameFactory> map = new EnumMap<>(MiniGameType.class);
        factories.forEach(factory -> map.put(factory.type(), factory));
        return map;
    }
}
