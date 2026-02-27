package pt.isel.api

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class CustomExceptionHandler : ResponseEntityExceptionHandler() {
    @Suppress("UNCHECKED_CAST")
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.info("Handling MethodArgumentNotValidException: {}", ex.message)
        return Problem.InvalidRequestContent.toResponseEntity() as ResponseEntity<Any>
    }

    @Suppress("UNCHECKED_CAST")
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        log.info("Handling HttpMessageNotReadableException: {}", ex.message)
        return Problem.InvalidRequestContent.toResponseEntity() as ResponseEntity<Any>
    }

    @ExceptionHandler(Exception::class)
    @Suppress("UNCHECKED_CAST")
    fun handleAll(
        req: HttpServletRequest,
        ex: Exception,
    ): ResponseEntity<Any> {
        log.error("Request: ${req.requestURL} raised $ex", ex)
        return Problem.InternalServerError.toResponseEntity() as ResponseEntity<Any>
    }

    companion object {
        private val log = LoggerFactory.getLogger(CustomExceptionHandler::class.java)
    }
}
