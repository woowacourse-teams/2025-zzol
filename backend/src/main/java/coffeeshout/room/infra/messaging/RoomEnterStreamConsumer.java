package coffeeshout.room.infra.messaging;

import coffeeshout.global.config.properties.RedisStreamProperties;
import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.MenuCommandService;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.ui.request.SelectedMenuRequest;
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
public class RoomEnterStreamConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    private final RoomCommandService roomCommandService;
    private final MenuCommandService menuCommandService;
    private final RoomEventWaitManager roomEventWaitManager;
    private final StreamMessageListenerContainer<String, ObjectRecord<String, String>> roomEnterStreamContainer;
    private final RedisStreamProperties redisStreamProperties;
    private final ObjectMapper objectMapper;

    public RoomEnterStreamConsumer(
            RoomCommandService roomCommandService, MenuCommandService menuCommandService,
            RoomEventWaitManager roomEventWaitManager,
            @Qualifier("roomEnterStreamContainer") StreamMessageListenerContainer<String, ObjectRecord<String, String>> roomEnterStreamContainer,
            RedisStreamProperties redisStreamProperties, ObjectMapper objectMapper
    ) {
        this.roomCommandService = roomCommandService;
        this.menuCommandService = menuCommandService;
        this.roomEventWaitManager = roomEventWaitManager;
        this.roomEnterStreamContainer = roomEnterStreamContainer;
        this.redisStreamProperties = redisStreamProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void registerListener() {
        roomEnterStreamContainer.receive(
                StreamOffset.fromStart(redisStreamProperties.roomJoinKey()),
                this
        );

        log.info("방 입장 스트림 리스너 등록 완료: {}", redisStreamProperties.roomJoinKey());
    }

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        log.info("방 입장 메시지 수신: messageId={}", message.getId());
        final RoomJoinEvent event = parseEvent(message);

        log.info("방 입장 메시지 eventId={}, joinCode={}, guestName={}",
                event.eventId(), event.joinCode(), event.guestName());

        try {
            final SelectedMenuRequest selectedMenuRequest = event.selectedMenuRequest();

            final Menu menu = menuCommandService.convertMenu(
                    selectedMenuRequest.id(),
                    selectedMenuRequest.customName()
            );

            final Room room = roomCommandService.joinGuest(
                    new JoinCode(event.joinCode()),
                    new PlayerName(event.guestName()),
                    menu, selectedMenuRequest.temperature()
            );

            log.info("방 입장 성공: joinCode={}, guestName={}, 현재 인원={}, eventId={}",
                    event.joinCode(), event.guestName(), room.getPlayers().size(), event.eventId());

            roomEventWaitManager.notifySuccess(event.eventId(), room);
        } catch (InvalidArgumentException | InvalidStateException e) {
            log.warn("방 입장 처리 오류: joinCode={}, guestName={}, eventId={}, messageId={}",
                    event.joinCode(), event.guestName(), event.eventId(), message.getId(), e);
            roomEventWaitManager.notifyFailure(event.eventId(), e);
        } catch (Exception e) {
            log.error("방 입장 처리 실패: joinCode={}, guestName={}, eventId={}, messageId={}, error={}",
                    event.joinCode(), event.guestName(), event.eventId(), message.getId(), e.getMessage(), e);
            roomEventWaitManager.notifyFailure(event.eventId(), e);
        }
    }

    private RoomJoinEvent parseEvent(ObjectRecord<String, String> message) {
        try {
            final String value = message.getValue();
            return objectMapper.readValue(value, RoomJoinEvent.class);
        } catch (JsonProcessingException e) {
            log.error("RoomJoinEvent 파싱 실패: messageId={}, messageValue={}, error={}",
                    message.getId(), message.getValue(), e.getMessage(), e);
            throw new IllegalArgumentException("이벤트 파싱 중 오류 발생: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("RoomJoinEvent 오류 발생: messageId={}, messageValue={}, error={}",
                    message.getId(), message.getValue(), e.getMessage(), e);
            throw new IllegalArgumentException("이벤트 파싱 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
