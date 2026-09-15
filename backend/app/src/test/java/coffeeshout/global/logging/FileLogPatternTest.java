package coffeeshout.global.logging;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 파일 로그 패턴이 엔트리 하나를 반드시 한 줄로 쓰는지 고정한다.
 *
 * <p>Alloy 가 application.log 를 줄 단위로 읽어 정규식으로 자른다. 요청 파라미터에 든 개행이
 * 그대로 찍히면 요청 한 번으로 가짜 ERROR 줄을 여러 개 심을 수 있고, 그 줄들이 Loki 알럿과
 * zzol-bot 분석을 오염시킨다. 패턴은 logback-spring.xml 에서 직접 읽는다. 여기 복사해 두면
 * 설정을 고쳐도 테스트가 옛 패턴을 검사한다.
 */
class FileLogPatternTest {

    @Test
    void 메시지와_예외의_개행을_지워_한_줄로_쓴다() throws Exception {
        // 새 LoggerContext 에는 MDC 어댑터가 없어 %X 가 NPE 를 낸다. 실제 컨텍스트를 빌려 쓴다.
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern(fileLogPattern());
        layout.start();

        final LoggingEvent event = new LoggingEvent(
                Logger.class.getName(),
                context.getLogger("coffeeshout.room.RoomService"),
                Level.WARN,
                "playerName=x\n[2026-09-15 00:00:00.000] [ERROR] --- [main] fake : 가짜 줄",
                new IllegalArgumentException("입력값: '\r\n[ERROR] 가짜 예외'"),
                null);

        final String line = layout.doLayout(event);
        final String body =
                line.substring(0, line.length() - System.lineSeparator().length());

        assertSoftly(softly -> {
            softly.assertThat(line).endsWith(System.lineSeparator());
            softly.assertThat(body)
                    .doesNotContain("\n")
                    .doesNotContain("\r")
                    .contains("playerName=x")
                    .contains("가짜 줄")
                    .contains("IllegalArgumentException")
                    .contains("가짜 예외");
        });
    }

    private static String fileLogPattern() throws Exception {
        try (InputStream xml = FileLogPatternTest.class.getResourceAsStream("/logback-spring.xml")) {
            final NodeList properties = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(xml)
                    .getElementsByTagName("property");
            for (int i = 0; i < properties.getLength(); i++) {
                final Element property = (Element) properties.item(i);
                if ("FILE_LOG_PATTERN".equals(property.getAttribute("name"))) {
                    return property.getAttribute("value");
                }
            }
        }
        throw new IllegalStateException("logback-spring.xml 에 FILE_LOG_PATTERN 이 없다");
    }
}
