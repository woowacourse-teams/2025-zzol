package coffeeshout.cardgame.domain;

import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.CardGameDeckGenerator;
import coffeeshout.cardgame.domain.card.Deck;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class CardGame implements Playable {

    private static final int ADDITION_CARD_COUNT = 7;
    private static final int MULTIPLIER_CARD_COUNT = 2;
    static final int TOTAL_ROUNDS = 2;

    private final Deck deck;
    private final long seed;
    private PlayerHands playerHands;
    private CardGameRound round;
    private CardGameState state;

    public CardGame(@NonNull CardGameDeckGenerator deckGenerator, long seed) {
        this.round = CardGameRound.ready(TOTAL_ROUNDS);
        this.state = CardGameState.READY;
        this.seed = seed;
        this.deck = deckGenerator.generate(ADDITION_CARD_COUNT, MULTIPLIER_CARD_COUNT, seed);
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromDescending(getScores());
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.CARD_GAME;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        playerHands = new PlayerHands(gamers);
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        return new HashMap<>(playerHands.scoreByPlayer());
    }

    public void startRound() {
        this.round = round.next();
        this.state = round.isFirst() ? CardGameState.FIRST_LOADING : CardGameState.LOADING;
    }

    public void updateDescription() {
        this.state = CardGameState.PREPARE;
    }

    public void startPlay() {
        // 시드 기반 셔플로 모든 인스턴스에서 동일한 카드 순서
        final Random random = new Random(seed + 1000); // 덱 생성과 구별하기 위해 오프셋 추가
        deck.shuffle(random);
        this.state = CardGameState.PLAYING;
    }

    public boolean selectCard(Gamer gamer, Integer cardIndex) {
        if (state != CardGameState.PLAYING) {
            throw new BusinessException(
                    CardGameErrorCode.NOT_PLAYING_STATE,
                    "현재 게임이 진행중인 상태가 아닙니다. state=" + state
            );
        }

        playerHands.put(gamer, deck.pick(cardIndex));
        return playerHands.isRoundFinished(this.round);
    }

    public boolean isFinishedThisRound() {
        return playerHands.isRoundFinished(this.round);
    }

    public boolean isFirstRound() {
        return round.isFirst();
    }

    public boolean isLastRound() {
        return round.isLast();
    }

    public int getTotalRounds() {
        return round.getTotalRounds();
    }

    public Gamer findByName(String name) {
        return playerHands.findByName(name);
    }

    public void assignRandomCardsToUnselectedPlayers() {
        final List<Gamer> unselectedPlayers = playerHands.getUnselectedPlayers(round);
        // 라운드 정보를 포함한 시드로 일관된 랜덤 생성
        final Random random = new Random(seed + round.toIndex());

        for (Gamer gamer : unselectedPlayers) {
            final Card card = deck.pickRandom(random);
            playerHands.put(gamer, card);
        }
    }

    public Optional<Gamer> findCardOwnerInCurrentRound(Card card) {
        return playerHands.findCardOwner(card, round);
    }

    public void changeScoreBoardState() {
        this.state = CardGameState.SCORE_BOARD;
    }

    public void changeDoneState() {
        this.state = CardGameState.DONE;
    }
}
