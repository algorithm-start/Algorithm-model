package com.recplatform.common.exception;

import com.recplatform.common.result.Result;
import com.recplatform.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle BusinessException.
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Business exception on [{} {}]: code={}, message={}",
                request.getMethod(), request.getRequestURI(),
                e.getResultCode().getCode(), e.getMessage());
        return Result.fail(e.getResultCode());
    }

    /**
     * Handle validation errors from @Valid request body.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation error on [{} {}]: {}", request.getMethod(), request.getRequestURI(), fieldErrors);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), fieldErrors);
    }

    /**
     * Handle constraint violation from @Validated path/query params.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        String violations = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation on [{} {}]: {}", request.getMethod(), request.getRequestURI(), violations);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), violations);
    }

    /**
     * Handle unreadable HTTP message (malformed JSON body).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("Malformed request body on [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST);
    }

    /**
     * Handle missing servlet request parameter.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("Missing request parameter on [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * Handle 404 Not Found.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("No handler found for [{} {}]", e.getHttpMethod(), e.getRequestURL());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /**
     * Handle unsupported media type.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("Unsupported media type on [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "Unsupported media type");
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Detects Sa-Token auth exceptions by class name to avoid hard dependency on sa-token in common module.
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        String exClassName = e.getClass().getName();

        // Sa-Token not-login exception → 401
        if (exClassName.contains("NotLoginException") || exClassName.contains("NotBasicAuthException")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            log.warn("Unauthorized access on [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            return Result.fail(ResultCode.UNAUTHORIZED.getCode(), "Unauthorized: " + e.getMessage());
        }

        // Sa-Token permission/role denied → 403
        if (exClassName.contains("NotPermissionException") || exClassName.contains("NotRoleException")) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            log.warn("Access denied on [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            return Result.fail(ResultCode.FORBIDDEN.getCode(), "Forbidden: " + e.getMessage());
        }

        // All other unexpected exceptions → 500
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        log.error("Unexpected error on [{} {}]", request.getMethod(), request.getRequestURI(), e);
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), "Internal Server Error");
    }
}
