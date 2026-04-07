package coffeeshout.global.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpExtractorTest {

    @Nested
    @DisplayName("extract")
    class Extract {

        @Test
        void XForwardedFor_헤더가_없으면_RemoteAddr를_반환한다() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");

            assertThat(ClientIpExtractor.extract(request)).isEqualTo("10.0.0.1");
        }

        @Test
        void XForwardedFor_헤더가_단일_IP이면_해당_IP를_반환한다() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "203.0.113.1");

            assertThat(ClientIpExtractor.extract(request)).isEqualTo("203.0.113.1");
        }

        @Test
        void XForwardedFor_헤더에_복수_IP가_있으면_마지막_IP를_반환한다() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.2, 172.16.0.1");

            assertThat(ClientIpExtractor.extract(request)).isEqualTo("172.16.0.1");
        }

        @Test
        void XForwardedFor가_unknown이면_RemoteAddr를_반환한다() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", "unknown");

            assertThat(ClientIpExtractor.extract(request)).isEqualTo("10.0.0.1");
        }

        @Test
        void XForwardedFor가_공백이면_RemoteAddr를_반환한다() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", "   ");

            assertThat(ClientIpExtractor.extract(request)).isEqualTo("10.0.0.1");
        }
    }
}
