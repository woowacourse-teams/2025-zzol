package coffeeshout.user.config;

import coffeeshout.user.ui.resolver.AuthenticatedUserArgumentResolver;
import coffeeshout.user.ui.resolver.RoomSessionClaimArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;
    private final RoomSessionClaimArgumentResolver roomSessionClaimArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedUserArgumentResolver);
        resolvers.add(roomSessionClaimArgumentResolver);
    }
}
