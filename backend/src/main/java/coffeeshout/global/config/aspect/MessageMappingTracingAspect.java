package coffeeshout.global.config.aspect;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageMappingTracingAspect {

    private final ObservationRegistry observationRegistry;

    @Around("@annotation(org.springframework.messaging.handler.annotation.MessageMapping)")
    public Object traceMessageMapping(ProceedingJoinPoint joinPoint) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method method = signature.getMethod();

        final String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        final String spanName = String.format("websocket: %s", methodName);

        return Observation.createNotStarted(spanName, observationRegistry)
                .lowCardinalityKeyValue("method.name", methodName)
                .lowCardinalityKeyValue("class.name", className)
                .observe(() -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable e) {
                        if (e instanceof RuntimeException) {
                            throw (RuntimeException) e;
                        }
                        throw new RuntimeException(e);
                    }
                });
    }
}
