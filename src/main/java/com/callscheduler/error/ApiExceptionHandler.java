package com.callscheduler.error;

import java.time.DateTimeException;
import java.util.stream.Collectors;

import org.hibernate.exception.DataException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Дата из запроса не помещается в календарь java.time: арифметика над ней переполнилась
    @ExceptionHandler(DateTimeException.class)
    ProblemDetail handleDateOverflow() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Дата вне допустимого диапазона");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // СУБД не приняла само значение из запроса, например дату за пределами timestamptz
        if (ex.contains(DataException.class)) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Запрос содержит недопустимое значение");
        }
        // Страховка от гонок: сработало ограничение БД (уникальный БИН, пересечение встреч менеджера)
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Запрос конфликтует с существующими данными");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = ex.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return badRequest(ex, detail, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = ex.getCause() instanceof JacksonException cause && !cause.getPath().isEmpty()
                ? "Некорректное значение поля «%s»".formatted(cause.getPath().getLast().getPropertyName())
                : "Не удалось прочитать тело запроса";
        return badRequest(ex, detail, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "Некорректное значение параметра «%s»".formatted(ex.getPropertyName());
        return badRequest(ex, detail, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        String detail = "Не указан обязательный параметр «%s»".formatted(ex.getParameterName());
        return badRequest(ex, detail, headers, request);
    }

    private ResponseEntity<Object> badRequest(Exception ex, String detail, HttpHeaders headers, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }
}
