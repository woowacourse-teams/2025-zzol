package coffeeshout.user.ui.resolver;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.exception.UserErrorCode;
import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class)
               && (parameter.getParameterType().equals(Optional.class)
                   || parameter.getParameterType().equals(AuthenticatedUser.class));
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final boolean isAuthenticated = authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser;

        if (parameter.getParameterType().equals(AuthenticatedUser.class)) {
            if (!isAuthenticated) {
                throw new BusinessException(UserErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
            }
            return (AuthenticatedUser) authentication.getPrincipal();
        }

        if (!isAuthenticated) {
            return Optional.empty();
        }
        return Optional.of((AuthenticatedUser) authentication.getPrincipal());
    }
}
