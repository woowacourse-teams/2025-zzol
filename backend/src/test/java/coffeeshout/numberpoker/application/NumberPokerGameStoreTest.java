package coffeeshout.numberpoker.application;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.numberpoker.domain.NumberPokerErrorCode;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NumberPokerGameStoreTest {

    private NumberPokerGameStore store;

    @BeforeEach
    void setUp() {
        store = new NumberPokerGameStore();
    }

    @Nested
    class 게임_저장_및_조회 {

        @Test
        void 저장된_게임을_joinCode로_조회할_수_있다() {
            NumberPokerGame game = new NumberPokerGame(
                    List.of(PlayerFixture.호스트꾹이(), PlayerFixture.게스트루키()));
            store.save("ABCD", game);

            assertThat(store.get("ABCD")).isSameAs(game);
        }

        @Test
        void 존재하지_않는_joinCode_조회시_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> store.get("NOTFOUND"),
                    NumberPokerErrorCode.GAME_NOT_FOUND
            );
        }
    }

    @Nested
    class 게임_삭제 {

        @Test
        void 게임을_삭제하면_이후_조회시_예외가_발생한다() {
            NumberPokerGame game = new NumberPokerGame(List.of(PlayerFixture.호스트꾹이()));
            store.save("ABCD", game);
            store.remove("ABCD");

            assertCoffeeShoutException(
                    () -> store.get("ABCD"),
                    NumberPokerErrorCode.GAME_NOT_FOUND
            );
        }
    }
}
