package coffeeshout.minigame.ui.request;

import coffeeshout.minigame.ui.command.MiniGameCommand;
import coffeeshout.minigame.ui.request.command.SelectCardCommand;
import coffeeshout.minigame.ui.request.command.StartMiniGameCommand;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * {@code {"commandType": "SELECT_CARD", "commandRequest": {...}}} 를 받는다. commandType 이 commandRequest 의
 * 구체 타입을 정한다. 카탈로그가 이 매핑을 읽어 FE 에 commandType 별 body 타입을 낸다.
 */
public record MiniGameMessage(
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "commandType")
        @JsonSubTypes({
            @JsonSubTypes.Type(value = StartMiniGameCommand.class, name = "START_MINI_GAME"),
            @JsonSubTypes.Type(value = SelectCardCommand.class, name = "SELECT_CARD")
        })
        MiniGameCommand commandRequest) {}
