package coffeeshout.cardgame.domain;

import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.CardGameDeckGenerator;
import coffeeshout.cardgame.domain.card.Deck;
import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.Playable;
import coffeeshout.room.domain.RoomErrorCode;
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
    private List<Gamer> gamers;

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
        this.gamers = List.copyOf(gamers);
        playerHands = new PlayerHands(gamers);
    }

    @Override
    public List<Gamer> getGamers() {
        return gamers;
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        return playerHands.scoreByGamer();
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
        gamer.validateAgainst(this.gamers);
        playerHands.putByGamer(gamer, deck.pick(cardIndex));
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

    public Gamer findGamer(Gamer gamer) {
        if (!playerHands.containsGamer(gamer)) {
            throw new BusinessException(
                    RoomErrorCode.NO_EXIST_PLAYER,
                    "해당 플레이어를 찾을 수 없습니다. name: " + gamer.name()
            );
        }
        return gamer;
    }

    public void assignRandomCardsToUnselectedPlayers() {
        final List<Gamer> unselectedGamers = playerHands.getUnselectedGamers(round);
        // 라운드 정보를 포함한 시드로 일관된 랜덤 생성
        final Random random = new Random(seed + round.toIndex());

        for (Gamer gamer : unselectedGamers) {
            final Card card = deck.pickRandom(random);
            playerHands.putByGamer(gamer, card);
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
