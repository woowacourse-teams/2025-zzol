package coffeeshout.websocket.docs;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

@Configuration
@Profile("!prod")
@RequiredArgsConstructor
public class WsCatalogSecurityConfig {

    private final WsCatalogProperties properties;
    private List<IpAddressMatcher> matchers;

    @PostConstruct
    public void init() {
        matchers = properties.allowedIps().stream()
                .map(IpAddressMatcher::new)
                .toList();
    }

    @Bean
    @Order(0)
    public SecurityFilterChain devCatalogFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/dev/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(request -> matchers.stream().anyMatch(m -> m.matches(request)))
                        .permitAll()
                        .anyRequest().denyAll()
                )
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
