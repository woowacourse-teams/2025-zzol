package coffeeshout.user.ui.resolver;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.websocket.auth.RoomSessionClaim;
import coffeeshout.websocket.auth.RoomSessionTokenErrorCode;
import coffeeshout.websocket.auth.RoomSessionTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class RoomSessionClaimArgumentResolver implements HandlerMethodArgumentResolver {

    private final RoomSessionTokenService roomSessionTokenService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RoomSession.class)
               && parameter.getParameterType().equals(RoomSessionClaim.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        final HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        final String roomToken = request != null ? request.getHeader("roomToken") : null;

        if (roomToken == null || roomToken.isBlank()) {
            throw new BusinessException(
                    RoomSessionTokenErrorCode.ROOM_TOKEN_MISSING,
                    RoomSessionTokenErrorCode.ROOM_TOKEN_MISSING.getMessage()
            );
        }

        return roomSessionTokenService.verify(roomToken);
    }
}
