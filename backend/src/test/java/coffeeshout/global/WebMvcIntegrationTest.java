package coffeeshout.global;

import coffeeshout.global.config.IntegrationTestConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTestConfig.class)
@ActiveProfiles("test")
@AutoConfigureWebMvc
@Transactional
public abstract class WebMvcIntegrationTest {

    @MockitoBean
    protected SimpMessagingTemplate messagingTemplate;
}
