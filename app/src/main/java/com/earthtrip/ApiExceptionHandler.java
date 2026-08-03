package com.earthtrip;

import com.earthtrip.sharedkernel.error.EarthTripException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(EarthTripException.class)
    ResponseEntity<ProblemDetail> handleEarthTrip(
        EarthTripException exception,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(exception.httpStatus()),
            exception.getMessage()
        );
        enrich(problem, exception.code(), request);
        exception.properties().forEach(problem::setProperty);
        return ResponseEntity.status(exception.httpStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "요청 값을 확인해 주세요."
        );
        enrich(problem, "VALIDATION_FAILED", request);
        problem.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        IllegalArgumentException.class
    })
    ResponseEntity<ProblemDetail> handleBadRequest(
        Exception exception,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            exception instanceof HttpMessageNotReadableException
                ? "요청 본문을 읽을 수 없습니다."
                : exception.getMessage()
        );
        enrich(problem, "INVALID_REQUEST", request);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(
        ObjectOptimisticLockingFailureException exception,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "다른 변경이 먼저 저장되었습니다. 최신 데이터를 다시 확인해 주세요."
        );
        enrich(problem, "VERSION_CONFLICT", request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataConflict(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "이미 처리되었거나 현재 데이터와 충돌하는 요청입니다."
        );
        enrich(problem, "DATA_CONFLICT", request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    private static void enrich(
        ProblemDetail problem,
        String code,
        HttpServletRequest request
    ) {
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
}
