package pt.isel.api.common;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pt.isel.domain.common.AppError;
import pt.isel.domain.common.ChannelError;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.InvitationError;
import pt.isel.domain.common.MessageError;
import pt.isel.domain.common.UserError;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingTest {

    @Test
    void testHandleResultSuccess() {
        Either<UserError, String> result = Either.success("Success Data");
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Success Data");
    }

    @Test
    void testHandleResultSuccessWithCustomMapper() {
        Either<UserError, String> result = Either.success("Created Data");
        ResponseEntity<?> response = ErrorHandling.handleResult(result, data -> ResponseEntity.status(HttpStatus.CREATED).body(data));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("Created Data");
    }

    @Test
    void testHandleResultFailureUserNotFound() {
        Either<UserError, String> result = Either.failure(new UserError.UserNotFound());
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isNotNull()
                .isInstanceOf(ProblemResponse.class)
                .asInstanceOf(InstanceOfAssertFactories.type(ProblemResponse.class))
                .extracting(ProblemResponse::title)
                .isEqualTo("User Not Found");
    }

    @Test
    void testHandleResultFailureChannelIsPrivate() {
        Either<ChannelError, String> result = Either.failure(new ChannelError.ChannelIsPrivate());
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .isNotNull()
                .isInstanceOf(ProblemResponse.class)
                .asInstanceOf(InstanceOfAssertFactories.type(ProblemResponse.class))
                .extracting(ProblemResponse::title)
                .isEqualTo("Channel Is Private");
    }

    @Test
    void testHandleResultMessageError() {
        Either<MessageError, String> result = Either.failure(new MessageError.EmptyMessage());
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .isInstanceOf(ProblemResponse.class)
                .asInstanceOf(InstanceOfAssertFactories.type(ProblemResponse.class))
                .extracting(ProblemResponse::title)
                .isEqualTo("Empty Message");
    }

    @Test
    void testHandleResultInvitationError() {
        Either<InvitationError, String> result = Either.failure(new InvitationError.InvitationAlreadyExists());
        ResponseEntity<?> response = ErrorHandling.handleResult(result);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .isNotNull()
                .isInstanceOf(ProblemResponse.class)
                .asInstanceOf(InstanceOfAssertFactories.type(ProblemResponse.class))
                .extracting(ProblemResponse::title)
                .isEqualTo("Invitation Already Exists");
    }

    @Test
    void testExhaustiveErrorMapping() throws Exception {
        List<Class<?>> errorClasses = getConcreteClasses(AppError.class);
        assertThat(errorClasses).as("Should find concrete error classes").isNotEmpty();

        for (Class<?> errorClass : errorClasses) {
            AppError errorInstance = (AppError) errorClass.getDeclaredConstructor().newInstance();
            Either<AppError, String> result = Either.failure(errorInstance);

            ResponseEntity<?> response = ErrorHandling.handleResult(result);

            assertThat(response).isNotNull();
            assertThat(response.getBody())
                    .as("Error " + errorClass.getSimpleName() + " should map to a ProblemResponse")
                    .isInstanceOf(ProblemResponse.class);
        }
    }

    private List<Class<?>> getConcreteClasses(Class<?> sealedInterface) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> permitted : sealedInterface.getPermittedSubclasses()) {
            if (permitted.isSealed()) {
                result.addAll(getConcreteClasses(permitted));
            } else {
                result.add(permitted);
            }
        }
        return result;
    }
}