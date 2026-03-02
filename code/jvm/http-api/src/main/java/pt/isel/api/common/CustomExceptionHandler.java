package pt.isel.api.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.info("Handling MethodArgumentNotValidException: {}", ex.getMessage());
        return new ResponseEntity<>(Problem.InvalidRequestContent.toResponseEntity().getBody(), Problem.InvalidRequestContent.status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.info("Handling HttpMessageNotReadableException: {}", ex.getMessage());
        return new ResponseEntity<>(Problem.InvalidRequestContent.toResponseEntity().getBody(), Problem.InvalidRequestContent.status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(HttpServletRequest req, Exception ex) {
        log.error("Request: {} raised {}", req.getRequestURL(), ex.toString(), ex);
        return new ResponseEntity<>(Problem.InternalServerError.toResponseEntity().getBody(), Problem.InternalServerError.status);
    }
}