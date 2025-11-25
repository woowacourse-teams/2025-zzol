package coffeeshout.cardgame.infra.messaging;

import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import coffeeshout.cardgame.domain.service.CardGameCommandService;
import coffeeshout.global.config.properties.RedisStreamProperties;
import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CardSelectStreamConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    private final CardGameCommandService cardGameCommandService;
    private final StreamMessageListenerContainer<String, ObjectRecord<String, String>> cardSelectStreamContainer;
    private final RedisStreamProperties redisStreamProperties;
    private final ObjectMapper objectMapper;

    public CardSelectStreamConsumer(
            CardGameCommandService cardGameCommandService,
            @Qualifier("cardSelectStreamContainer") StreamMessageListenerContainer<String, ObjectRecord<String, String>> cardSelectStreamContainer,
            RedisStreamProperties redisStreamProperties, ObjectMapper objectMapper
    ) {
        this.cardGameCommandService = cardGameCommandService;
        this.cardSelectStreamContainer = cardSelectStreamContainer;
        this.redisStreamProperties = redisStreamProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void registerListener() {
        // 단독 소비자 패턴으로 스트림 리스너 등록
        cardSelectStreamContainer.receive(
                StreamOffset.fromStart(redisStreamProperties.cardGameSelectKey()),
                this
        );

        log.info("카드 선택 스트림 리스너 등록 완료: {}", redisStreamProperties.cardGameSelectKey());
    }

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        final SelectCardCommandEvent event = parseEvent(message);
        log.info("카드 선택 메시지 수신: messageId={}, joinCode={}, playerName={}, cardIndex={}",
                message.getId(), event.joinCode(), event.playerName(), event.cardIndex());

        try {
            final JoinCode joinCode = new JoinCode(event.joinCode());
            final PlayerName playerName = new PlayerName(event.playerName());

            cardGameCommandService.selectCard(joinCode, playerName, event.cardIndex());

            log.info("카드 선택 처리 성공: joinCode={}, playerName={}, cardIndex={}, messageId={}",
                    event.joinCode(), event.playerName(), event.cardIndex(), message.getId());

        } catch (InvalidArgumentException | InvalidStateException e) {
            log.warn("카드 선택 처리 중 오류 발생: joinCode={}, playerName={}, cardIndex={}, messageId={}",
                    event.joinCode(), event.playerName(), event.cardIndex(), message.getId(), e);
        } catch (Exception e) {
            log.error("카드 선택 처리 실패: joinCode={}, playerName={}, cardIndex={}, messageId={}",
                    event.joinCode(), event.playerName(), event.cardIndex(), message.getId(), e);
        }
    }

    private SelectCardCommandEvent parseEvent(ObjectRecord<String, String> message) {
        try {
            final String value = message.getValue();
            return objectMapper.readValue(value, SelectCardCommandEvent.class);
        } catch (JsonProcessingException e) {
            log.error("SelectCardCommandEvent 파싱 실패: messageId={}, messageValue={}, error={}",
                    message.getId(), message.getValue(), e.getMessage(), e);
            throw new IllegalArgumentException("이벤트 파싱 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
