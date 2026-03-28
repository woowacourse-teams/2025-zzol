package coffeeshout.global.exception;

import static coffeeshout.global.log.LogAspect.NOTIFICATION_MARKER;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.exception.custom.InfrastructureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        logError(exception, request);
        return getProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception, GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        logWarning(exception, request);
        return getProblemDetail(HttpStatus.NOT_FOUND, exception, GlobalErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        logWarning(exception, request);
        return getProblemDetail(exception.getErrorCode().getHttpStatus(), exception, exception.getErrorCode());
    }

    @ExceptionHandler(InfrastructureException.class)
    public ProblemDetail handleInfrastructureException(
            InfrastructureException exception,
            HttpServletRequest request
    ) {
        logError(exception, request);
        return getProblemDetail(exception.getErrorCode().getHttpStatus(), exception, exception.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        logWarning(exception, request);

        String errorMessage = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse(GlobalErrorCode.VALIDATION_ERROR.getMessage());

        return getProblemDetail(HttpStatus.BAD_REQUEST, exception, new ErrorCode() {
            @Override public String getCode() { return GlobalErrorCode.VALIDATION_ERROR.getCode(); }
            @Override public String getMessage() { return errorMessage; }
            @Override public org.springframework.http.HttpStatus getHttpStatus() { return HttpStatus.BAD_REQUEST; }
        });
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        logWarning(exception, request);

        final String errorMessage = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        return getProblemDetail(HttpStatus.BAD_REQUEST, exception, new ErrorCode() {
            @Override public String getCode() { return GlobalErrorCode.CONSTRAINT_VIOLATION.getCode(); }
            @Override public String getMessage() { return errorMessage.isBlank() ? GlobalErrorCode.CONSTRAINT_VIOLATION.getMessage() : errorMessage; }
            @Override public org.springframework.http.HttpStatus getHttpStatus() { return HttpStatus.BAD_REQUEST; }
        });
    }

    private static ProblemDetail getProblemDetail(HttpStatus status, Exception exception, ErrorCode errorCode) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, errorCode.getMessage());

        problemDetail.setProperty("errorCode", errorCode.getCode());
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("exception", exception.getClass().getSimpleName());

        return problemDetail;
    }

    private void logError(
            final Exception e,
            final HttpServletRequest request
    ) {
        final String logMessage = String.format(
                "method=%s uri=%s exception=%s message=%s",
                request.getMethod(),
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                e.getMessage()
        );
        log.error(NOTIFICATION_MARKER, logMessage, e);
    }

    private void logWarning(
            final Exception e,
            final HttpServletRequest request
    ) {
        final String logMessage = String.format(
                "method=%s uri=%s exception=%s message=%s",
                request.getMethod(),
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                e.getMessage()
        );
        log.warn(logMessage, e);
    }
}
