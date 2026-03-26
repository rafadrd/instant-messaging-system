package pt.isel.api.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pt.isel.domain.common.ChannelError;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.InvitationError;
import pt.isel.domain.common.MessageError;
import pt.isel.domain.common.UserError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ErrorHandlingTest {

    @Test
    void testHandleResultSuccess() {
        Either<UserError, String> result = Either.success("Success Data");
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success Data", response.getBody());
    }

    @Test
    void testHandleResultSuccessWithCustomMapper() {
        Either<UserError, String> result = Either.success("Created Data");
        ResponseEntity<?> response = ErrorHandling.handleResult(result, data -> ResponseEntity.status(HttpStatus.CREATED).body(data));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Created Data", response.getBody());
    }

    @Test
    void testHandleResultFailureUserNotFound() {
        Either<UserError, String> result = Either.failure(new UserError.UserNotFound());
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertInstanceOf(ProblemResponse.class, response.getBody());
        ProblemResponse problem = (ProblemResponse) response.getBody();
        assertEquals("User Not Found", problem.title());
    }

    @Test
    void testHandleResultFailureChannelIsPrivate() {
        Either<ChannelError, String> result = Either.failure(new ChannelError.ChannelIsPrivate());
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertInstanceOf(ProblemResponse.class, response.getBody());
        ProblemResponse problem = (ProblemResponse) response.getBody();
        assertEquals("Channel Is Private", problem.title());
    }

    @Test
    void testHandleResultMessageError() {
        Either<MessageError, String> result = Either.failure(new MessageError.EmptyMessage());
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ProblemResponse.class, response.getBody());
        ProblemResponse problem = (ProblemResponse) response.getBody();
        assertEquals("Empty Message", problem.title());
    }

    @Test
    void testHandleResultInvitationError() {
        Either<InvitationError, String> result = Either.failure(new InvitationError.InvitationAlreadyExists());
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertInstanceOf(ProblemResponse.class, response.getBody());
        ProblemResponse problem = (ProblemResponse) response.getBody();
        assertEquals("Invitation Already Exists", problem.title());
    }
}