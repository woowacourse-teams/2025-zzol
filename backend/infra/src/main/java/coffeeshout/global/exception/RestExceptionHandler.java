package coffeeshout.global.exception;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.global.exception.custom.SystemException;
import coffeeshout.global.ipblock.IpBlockAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import coffeeshout.global.log.NotificationMarker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception exception, HttpServletRequest request) {
        logError(exception, request);
        return getProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception, GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFoundException(NoResourceFoundException exception, HttpServletRequest request) {
        logWarning(exception, request);
        return getProblemDetail(HttpStatus.NOT_FOUND, exception, GlobalErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException exception, HttpServletRequest request) {
        logWarning(exception, request);
        final ErrorCode errorCode = exception.getErrorCode();
        final HttpStatus httpStatus = HttpStatus.resolve(errorCode.getStatusCode());
        if (httpStatus == HttpStatus.NOT_FOUND) {
            request.setAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND, true);
        }
        return getProblemDetail(httpStatus, exception, errorCode);
    }

    @ExceptionHandler(SystemException.class)
    public ProblemDetail handleSystemException(SystemException exception, HttpServletRequest request) {
        logError(exception, request);
        return getProblemDetail(toStatus(exception.getErrorCode()), exception, exception.getErrorCode());
    }

    @ExceptionHandler(InfrastructureException.class)
    public ProblemDetail handleInfrastructureException(InfrastructureException exception, HttpServletRequest request) {
        logError(exception, request);
        return getProblemDetail(toStatus(exception.getErrorCode()), exception, exception.getErrorCode());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, HttpServletRequest request) {
        logWarning(exception, request);
        return getProblemDetail(HttpStatus.BAD_REQUEST, exception, GlobalErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        logWarning(exception, request);
        final String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse(GlobalErrorCode.VALIDATION_ERROR.getMessage());
        return getProblemDetail(HttpStatus.BAD_REQUEST, exception, toErrorCode(errorMessage, GlobalErrorCode.VALIDATION_ERROR));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException exception, HttpServletRequest request) {
        logWarning(exception, request);
        final String errorMessage = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return getProblemDetail(HttpStatus.BAD_REQUEST, exception, toErrorCode(errorMessage, GlobalErrorCode.CONSTRAINT_VIOLATION));
    }

    private static HttpStatus toStatus(ErrorCode errorCode) {
        return HttpStatus.resolve(errorCode.getStatusCode());
    }

    private static ProblemDetail getProblemDetail(HttpStatus status, Exception exception, ErrorCode errorCode) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, errorCode.getMessage());
        problemDetail.setProperty("errorCode", errorCode.getCode());
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("exception", exception.getClass().getSimpleName());
        return problemDetail;
    }

    private static ErrorCode toErrorCode(String errorMessage, GlobalErrorCode fallback) {
        return new ErrorCode() {
            @Override public String getCode() { return fallback.getCode(); }
            @Override public String getMessage() { return errorMessage.isBlank() ? fallback.getMessage() : errorMessage; }
            @Override public int getStatusCode() { return fallback.getStatusCode(); }
        };
    }

    private void logError(Exception e, HttpServletRequest request) {
        log.error(NotificationMarker.INSTANCE, "method={} uri={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);
    }

    private void logWarning(Exception e, HttpServletRequest request) {
        log.warn("method={} uri={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
    }
}
