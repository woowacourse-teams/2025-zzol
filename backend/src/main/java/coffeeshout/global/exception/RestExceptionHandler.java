package coffeeshout.global.exception;

import static coffeeshout.global.log.LogAspect.NOTIFICATION_MARKER;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.global.exception.custom.NotExistElementException;
import coffeeshout.global.exception.custom.QRCodeGenerationException;
import coffeeshout.global.exception.custom.StorageServiceException;
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
        return getProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception, new ErrorCode() {
            @Override
            public String getCode() {
                return "INTERNAL_SERVER_ERROR";
            }

            @Override
            public String getMessage() {
                return "서버 오류가 발생했습니다.";
            }
        });
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        logWarning(exception, request);
        return getProblemDetail(HttpStatus.NOT_FOUND, exception, new ErrorCode() {
            @Override
            public String getCode() {
                return "RESOURCE_NOT_FOUND";
            }

            @Override
            public String getMessage() {
                return "요청한 리소스를 찾을 수 없습니다.";
            }
        });
    }

    @ExceptionHandler(InvalidArgumentException.class)
    public ProblemDetail handleInvalidArgumentException(
            InvalidArgumentException exception,
            HttpServletRequest request
    ) {
        logWarning(exception, request);
        return getProblemDetail(HttpStatus.BAD_REQUEST, exception, exception.getErrorCode());
    }

    @ExceptionHandler(InvalidStateException.class)
    public ProblemDetail handleInvalidStateException(
            InvalidStateException exception,
            HttpServletRequest request
    ) {
        logWarning(exception, request);
        return getProblemDetail(HttpStatus.CONFLICT, exception, exception.getErrorCode());
    }

    @ExceptionHandler(NotExistElementException.class)
    public ProblemDetail handleNotExistElementException(
            NotExistElementException exception,
            HttpServletRequest request
    ) {
        logWarning(exception, request);
        return getProblemDetail(HttpStatus.NOT_FOUND, exception, exception.getErrorCode());
    }

    @ExceptionHandler(QRCodeGenerationException.class)
    public ProblemDetail handleQRCodeGenerationException(QRCodeGenerationException exception,
                                                         HttpServletRequest request) {
        logError(exception, request);
        return getProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception, exception.getErrorCode());
    }

    @ExceptionHandler(StorageServiceException.class)
    public ProblemDetail handleStorageServiceException(StorageServiceException exception,
                                                       HttpServletRequest request) {
        logError(exception, request);
        return getProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, exception, exception.getErrorCode());
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
                .orElse("유효하지 않은 요청입니다.");

        return getProblemDetail(HttpStatus.BAD_REQUEST, exception, new ErrorCode() {
            @Override
            public String getCode() {
                return "VALIDATION_ERROR";
            }

            @Override
            public String getMessage() {
                return errorMessage;
            }
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
            @Override
            public String getCode() {
                return "CONSTRAINT_VIOLATION";
            }

            @Override
            public String getMessage() {
                return errorMessage.isBlank() ? "요청 파라미터가 유효하지 않습니다." : errorMessage;
            }
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

