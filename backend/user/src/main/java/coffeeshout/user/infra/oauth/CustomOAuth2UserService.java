package coffeeshout.user.infra.oauth;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final List<OAuth2UserConverter> converters;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        final OAuth2User oAuth2User = super.loadUser(userRequest);
        final String registrationId = userRequest.getClientRegistration().getRegistrationId();

        final OAuth2UserConverter converter = converters.stream()
                .filter(c -> c.provider().getRegistrationId().equals(registrationId))
                .findFirst()
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("provider_not_supported"),
                        "지원하지 않는 OAuth 제공자입니다: " + registrationId));

        final String providerUserId = converter.extractProviderUserId(oAuth2User);
        final String email = converter.extractEmail(oAuth2User);
        final String nickname = converter.extractNickname(oAuth2User);

        return new CustomOAuth2User(oAuth2User, registrationId, providerUserId, email, nickname);
    }

    public record CustomOAuth2User(
            OAuth2User delegate,
            String registrationId,
            String providerUserId,
            String email,
            String nickname
    ) implements OAuth2User {

        @Override
        public Map<String, Object> getAttributes() {
            return delegate.getAttributes();
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return delegate.getAuthorities();
        }

        @Override
        public String getName() {
            return providerUserId;
        }
    }
}
