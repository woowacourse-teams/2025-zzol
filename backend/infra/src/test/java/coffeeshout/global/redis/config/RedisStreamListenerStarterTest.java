package coffeeshout.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import coffeeshout.fixture.BaseEventDummy;
import coffeeshout.global.redis.EventDispatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.function.Predicate;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.springframework.util.ErrorHandler;

@ExtendWith(MockitoExtension.class)
class RedisStreamListenerStarterTest {

    private static final String STREAM_KEY = "room";

    @Mock
    private RedisStreamProperties properties;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private EventDispatcher eventDispatcher;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private GenericApplicationContext genericApplicationContext;

    private ObjectMapper objectMapper;
    private RedisStreamListenerStarter starter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerSubtypes(BaseEventDummy.class);

        starter = new RedisStreamListenerStarter(
                properties,
                redisConnectionFactory,
                objectMapper,
                eventDispatcher,
                applicationContext,
                genericApplicationContext
        );
    }

    @Nested
    class 구독_요청을_생성할_때 {

        @Test
        void 어떤_예외가_발생해도_구독을_취소하지_않는다() {
            // given
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);

            // when
            final Predicate<Throwable> cancelOnError = request.getCancelSubscriptionOnError();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cancelOnError.test(new RuntimeException("리스너 처리 실패"))).isFalse();
                softly.assertThat(cancelOnError.test(new RedisSystemException("연결 오류", new IOException()))).isFalse();
                softly.assertThat(cancelOnError.test(new Error("심각한 오류"))).isFalse();
            });
        }

        @Test
        void 컨텍스트_종료_중에는_구독_취소를_허용한다() {
            // given — 종료 중 취소를 막으면 파괴된 커넥션 팩토리에 폴링을 무한 재시도한다
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);
            starter.onContextClosed(new ContextClosedEvent(genericApplicationContext));

            // when & then
            assertThat(request.getCancelSubscriptionOnError().test(new RuntimeException("종료 중 오류"))).isTrue();
        }

        @Test
        void 스트림_키와_오류_핸들러가_설정된다() {
            // when
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(request.getStreamOffset().getKey()).isEqualTo(STREAM_KEY);
                softly.assertThat(request.getErrorHandler()).isNotNull();
            });
        }
    }

    @Nested
    class 스트림_오류를_처리할_때 {

        private Logger logger;
        private ListAppender<ILoggingEvent> appender;

        @BeforeEach
        void attachAppender() {
            logger = (Logger) LoggerFactory.getLogger(RedisStreamListenerStarter.class);
            appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
        }

        @AfterEach
        void detachAppender() {
            logger.detachAppender(appender);
        }

        @Test
        void 종료_중에는_어떤_오류도_ERROR로_기록하지_않는다() {
            // given
            final ErrorHandler errorHandler = starter.buildReadRequest(STREAM_KEY).getErrorHandler();
            starter.onContextClosed(new ContextClosedEvent(genericApplicationContext));

            // when
            errorHandler.handleError(
                    new IllegalStateException("LettuceConnectionFactory was destroyed and cannot be used anymore")
            );
            errorHandler.handleError(new RuntimeException("종료 중 임의 오류"));

            // then
            assertThat(appender.list).noneMatch(event -> event.getLevel() == Level.ERROR);
        }

        @Test
        void 평시_오류는_ERROR로_기록한다() {
            // given
            final ErrorHandler errorHandler = starter.buildReadRequest(STREAM_KEY).getErrorHandler();

            // when
            errorHandler.handleError(new RuntimeException("폴링 실패"));

            // then
            assertThat(appender.list).anyMatch(event -> event.getLevel() == Level.ERROR);
        }
    }

    @Nested
    class 메시지를_수신할_때 {

        @Test
        void 정상_메시지는_EventDispatcher에_위임한다() throws JsonProcessingException {
            // given
            final BaseEventDummy event = BaseEventDummy.페이로드("정상 메시지");
            final ObjectRecord<String, String> message = 메시지(objectMapper.writeValueAsString(event));

            // when
            starter.onMessage(message);

            // then
            final ArgumentCaptor<BaseEventDummy> captor = ArgumentCaptor.forClass(BaseEventDummy.class);
            verify(eventDispatcher).handle(captor.capture());
            assertThat(captor.getValue().eventId()).isEqualTo(event.eventId());
        }

        @Test
        void 역직렬화에_실패해도_예외를_전파하지_않는다() {
            // given
            final ObjectRecord<String, String> message = 메시지("json이 아닌 값");

            // when & then — 예외가 전파되면 Spring Data Redis가 구독을 취소할 수 있다
            assertThatCode(() -> starter.onMessage(message)).doesNotThrowAnyException();
            verify(eventDispatcher, never()).handle(any());
        }

        @Test
        void 이벤트_처리_중_예외가_발생해도_전파하지_않는다() throws JsonProcessingException {
            // given
            willThrow(new RuntimeException("처리 실패")).given(eventDispatcher).handle(any());
            final ObjectRecord<String, String> message = 메시지(
                    objectMapper.writeValueAsString(BaseEventDummy.페이로드("처리 실패 메시지"))
            );

            // when & then — 예외가 전파되면 Spring Data Redis가 구독을 취소할 수 있다
            assertThatCode(() -> starter.onMessage(message)).doesNotThrowAnyException();
        }
    }

    private ObjectRecord<String, String> 메시지(String value) {
        return StreamRecords.newRecord().in(STREAM_KEY).ofObject(value);
    }
}
