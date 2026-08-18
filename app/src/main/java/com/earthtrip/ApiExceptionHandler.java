package com.earthtrip;

import com.earthtrip.sharedkernel.error.EarthTripException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(EarthTripException.class)
    ResponseEntity<ProblemDetail> handleEarthTrip(
            EarthTripException exception, HttpServletRequest request) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatusCode.valueOf(exception.httpStatus()), exception.getMessage());
        enrich(problem, exception.code(), request);
        exception.properties().forEach(problem::setProperty);
        log(exception.httpStatus(), exception.code(), exception, request, false);
        return ResponseEntity.status(exception.httpStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 값을 확인해 주세요.");
        enrich(problem, "VALIDATION_FAILED", request);
        problem.setProperty("fieldErrors", fieldErrors);
        log(400, "VALIDATION_FAILED", exception, request, false);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        IllegalArgumentException.class
    })
    ResponseEntity<ProblemDetail> handleBadRequest(
            Exception exception, HttpServletRequest request) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception instanceof HttpMessageNotReadableException
                                ? "요청 본문을 읽을 수 없습니다."
                                : exception.getMessage());
        enrich(problem, "INVALID_REQUEST", request);
        log(400, "INVALID_REQUEST", exception, request, false);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT, "다른 변경이 먼저 저장되었습니다. 최신 데이터를 다시 확인해 주세요.");
        enrich(problem, "VERSION_CONFLICT", request);
        log(409, "VERSION_CONFLICT", exception, request, false);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataConflict(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT, "이미 처리되었거나 현재 데이터와 충돌하는 요청입니다.");
        enrich(problem, "DATA_CONFLICT", request);
        log(409, "DATA_CONFLICT", exception, request, true);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        enrich(problem, "INTERNAL_SERVER_ERROR", request);
        log(500, "INTERNAL_SERVER_ERROR", exception, request, true);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private static void enrich(ProblemDetail problem, String code, HttpServletRequest request) {
        problem.setTitle(HttpStatus.valueOf(problem.getStatus()).getReasonPhrase());
        problem.setType(URI.create("https://earthtrip.app/problems/" + code.toLowerCase()));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("traceId", traceId(request));
    }

    private static String traceId(HttpServletRequest request) {
        Object existing = request.getAttribute("earthTripTraceId");
        if (existing instanceof String value && !value.isBlank()) {
            return value;
        }
        String generated = UUID.randomUUID().toString();
        request.setAttribute("earthTripTraceId", generated);
        return generated;
    }

    private static void log(
            int status,
            String code,
            Exception exception,
            HttpServletRequest request,
            boolean includeStackForClientError) {
        String message =
                "API_ERROR code=%s status=%d method=%s route=%s traceId=%s exception=%s"
                        .formatted(
                                code,
                                status,
                                request.getMethod(),
                                route(request),
                                traceId(request),
                                exception.getClass().getName());
        if (status >= 500) {
            LOGGER.error(message, exception);
        } else if (includeStackForClientError) {
            LOGGER.warn(message, exception);
        } else {
            LOGGER.warn(message);
        }
    }

    private static String route(HttpServletRequest request) {
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return route == null ? "-" : route.toString().replaceAll("[\\r\\n\\t]", "_");
    }
}
