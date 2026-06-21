package coffeeshout.nunchi.application;

import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.nunchi.config.NunchiTimingProperties;
import coffeeshout.nunchi.domain.NunchiGame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 눈치게임 SPI 등록(ADR-0031 결정 1). 동시 판정 윈도우(300ms)를 설정에서 읽어 도메인에 주입한다.
 */
@Component
@RequiredArgsConstructor
public class NunchiGameFactory implements MiniGameFactory {

    private final NunchiTimingProperties timing;

    @Override
    public MiniGameType type() {
        return MiniGameType.NUNCHI_GAME;
    }

    @Override
    public Playable create(String joinCode) {
        return new NunchiGame(timing.numberWindow().toMillis());
    }
}
