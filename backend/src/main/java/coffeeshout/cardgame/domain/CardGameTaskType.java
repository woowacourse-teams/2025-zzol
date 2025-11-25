package coffeeshout.cardgame.domain;

import coffeeshout.cardgame.domain.event.dto.CardGameStateChangedEvent;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameTask;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.Room;
import java.time.Instant;
import java.util.Arrays;
import lombok.Getter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;

@Getter
public enum CardGameTaskType {

    FIRST_ROUND_LOADING(CardGameState.FIRST_LOADING, CardGameRound.FIRST) {
        @Override
        public void processTask(
                CardGame cardGame,
                Room room,
                TaskScheduler taskScheduler,
                ApplicationEventPublisher eventPublisher
        ) {
            final MiniGameTask miniGameTask = new MiniGameTask(() -> {
                cardGame.startRound();
                final CardGameStateChangedEvent event = new CardGameStateChangedEvent(room, cardGame, this);
                eventPublisher.publishEvent(event);
                nextTask().processTask(cardGame, room, taskScheduler, eventPublisher);
            }, generateCorrelationId(room, this));
            taskScheduler.schedule(miniGameTask, Instant.now());
        }
    },
    FIRST_ROUND_DESCRIPTION(CardGameState.PREPARE, CardGameRound.FIRST) {
        @Override
        public void processTask(
                CardGame cardGame,
                Room room,
                TaskScheduler taskScheduler,
                ApplicationEventPublisher eventPublisher
        ) {
            final MiniGameTask miniGameTask = new MiniGameTask(() -> {
                cardGame.updateDescription();
                final CardGameStateChangedEvent event = new CardGameStateChangedEvent(room, cardGame, this);
                eventPublisher.publishEvent(event);
                nextTask().processTask(cardGame, room, taskScheduler, eventPublisher);
            }, generateCorrelationId(room, this));
            taskScheduler.schedule(miniGameTask, Instant.now().plusMillis(previousTask().state.getDuration()));
        }
    },
    FIRST_ROUND_PLAYING(CardGameState.PLAYING, CardGameRound.FIRST) {
        @Override
        public void processTask(
                CardGame cardGame,
                Room room,
                TaskScheduler taskScheduler,
                ApplicationEventPublisher eventPublisher
        ) {
            final MiniGameTask miniGameTask = new MiniGameTask(() -> {
                cardGame.startPlay();
                final CardGameStateChangedEvent event = new CardGameStateChangedEvent(room, cardGame, this);
                eventPublisher.publishEvent(event);
                nextTask().processTask(cardGame, room, taskScheduler, eventPublisher);
            }, generateCorrelationId(room, this));
            taskScheduler.schedule(miniGameTask, Instant.now().plusMillis(previousTask().state.getDuration()));
        }
    },
    FIRST_ROUND_SCORE_BOARD(CardGameState.SCORE_BOARD, CardGameRound.FIRST) {
        @Override
        public void processTask(
                CardGame cardGame,
                Room room,
                TaskScheduler taskScheduler,
                ApplicationEventPublisher eventPublisher
        ) {
            final MiniGameTask miniGameTask = new MiniGameTask(() -> {
                cardGame.assignRandomCardsToUnselectedPlayers();
                cardGame.changeScoreBoardState();
                final CardGameStateChangedEvent event = new CardGameStateChangedEvent(room, cardGame, this);
                eventPublisher.publishEvent(event);
                nextTask().processTask(cardGame, room, taskScheduler, eventPublisher);
            }, generateCorrelationId(room, this));
            taskScheduler.schedule(miniGameTask, Instant.now().plusMillis(previousTask().state.getDuration()));
        }
    },
    SECOND_ROUND_LOADING(CardGameState.LOADING, CardGameRound.SECOND) {
        @Override
        public void processTask(
                CardGame cardGame,
                Room room,
                TaskScheduler taskScheduler,
                ApplicationEventPublisher eventPublisher
        ) {
            final MiniGameTask miniGameTask = new MiniGameTask(() -> {
                cardGame.startRound();
                final CardGameStateChangedEvent event = new CardGameStateChangedEvent(room, cardGame, this);
                eventPublisher.publishEvent(event);
                nextTask().processTask(cardGame, room, taskScheduler, eventPublisher);
            }, generateCorrelationId(room, this));
            taskScheduler.schedule(miniGameTask, Instant.now().plusMillis(previousTask().state.getDuration()));
        }
    },
    SECOND_ROUND_PLAYING(CardGameState.PLAYING, CardGameRound.SECOND) {
        @Override
        public void processTask(
                CardGame cardGame,
                Room room,
                TaskScheduler taskScheduler,
                ApplicationEventPublisher eventPublisher
        ) {
            final MiniGameTask miniGameTask = new MiniGameTask(() -> {
                cardGame.startPlay();
                final CardGameStateChangedEvent event = new CardGameStateChangedEvent(room, cardGame, this);
                eventPublisher.publishEvent(event);
                nextTask().processTask(cardGame, room, taskScheduler, eventPublisher);
            }, generateCorrelationId(room, this));
            taskScheduler.schedule(miniGameTask, Instant.now().plusMillis(previousTask().state.getDuration()));
        }
    },
    SECOND_ROUND_SCORE_BOARD(CardGameState.SCORE_BOARD, CardGameRound.SECOND) {
        @Override
        public void processTask(
                CardGame cardGame,
                Room room,
                TaskScheduler taskScheduler,
                ApplicationEventPublisher eventPublisher
        ) {
            final MiniGameTask miniGameTask = new MiniGameTask(() -> {
                cardGame.assignRandomCardsToUnselectedPlayers();
                cardGame.changeScoreBoardState();
                final CardGameStateChangedEvent event = new CardGameStateChangedEvent(room, cardGame, this);
                eventPublisher.publishEvent(event);
                nextTask().processTask(cardGame, room, taskScheduler, eventPublisher);
            }, generateCorrelationId(room, this));
            taskScheduler.schedule(miniGameTask, Instant.now().plusMillis(previousTask().state.getDuration()));
        }
    },
    GAME_FINISH_STATE(CardGameState.DONE, CardGameRound.SECOND) {
        @Override
        public void processTask(
                CardGame cardGame,
                Room room,
                TaskScheduler taskScheduler,
                ApplicationEventPublisher eventPublisher
        ) {
            final MiniGameTask miniGameTask = new MiniGameTask(() -> {
                cardGame.changeDoneState();
                MiniGameResult result = cardGame.getResult();
                room.applyMiniGameResult(result);
                final CardGameStateChangedEvent event = new CardGameStateChangedEvent(room, cardGame, this);
                final MiniGameFinishedEvent finishedEvent = new MiniGameFinishedEvent(
                        room.getJoinCode().getValue(),
                        MiniGameType.CARD_GAME.name()
                );
                eventPublisher.publishEvent(event);
                eventPublisher.publishEvent(finishedEvent);
            }, generateCorrelationId(room, this));
            taskScheduler.schedule(miniGameTask, Instant.now().plusMillis(previousTask().state.getDuration()));
        }
    },
    ;

    private final CardGameState state;
    private final CardGameRound round;

    public static CardGameTaskType from(CardGame cardGame) {
        return of(cardGame.getState(), cardGame.getRound());
    }

    public static CardGameTaskType getFirstTask() {
        return values()[0];
    }

    public abstract void processTask(
            CardGame cardGame,
            Room room,
            TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher
    );

    CardGameTaskType(CardGameState state, CardGameRound round) {
        this.state = state;
        this.round = round;
    }

    public static CardGameTaskType of(CardGameState state, CardGameRound round) {
        return Arrays.stream(values()).filter(type -> type.state == state && type.round == round)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카드게임 작업입니다."));
    }

    public boolean isLastTask() {
        return this.ordinal() == values().length - 1;
    }

    public boolean isFirstTask() {
        return this.ordinal() == 0;
    }

    public CardGameTaskType nextTask() {
        if (isLastTask()) {
            throw new IllegalArgumentException("마지막 작업입니다.");
        }
        return values()[this.ordinal() + 1];
    }

    public CardGameTaskType previousTask() {
        if (isFirstTask()) {
            throw new IllegalArgumentException("첫 번째 작업입니다.");
        }
        return values()[this.ordinal() - 1];
    }

    private static String generateCorrelationId(Room room, CardGameTaskType task) {
        return String.format("JoinCode: %s / gameState: %s", room.getJoinCode().getValue(), task.name());
    }
}
