package pt.isel.domain.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EitherTest {

    @Test
    void testSuccessCreation() {
        Either<String, Integer> result = Either.success(42);
        assertThat(EitherAssert.assertRight(result)).isEqualTo(42);
    }

    @Test
    void testFailureCreation() {
        Either<String, Integer> result = Either.failure("Error occurred");
        assertThat(EitherAssert.assertLeft(result)).isEqualTo("Error occurred");
    }

    @Test
    void testMapOnRight() {
        Either<String, Integer> success = Either.success(10);
        Either<String, String> mapped = success.map(val -> "Number " + val);

        assertThat(EitherAssert.assertRight(mapped)).isEqualTo("Number 10");
    }

    @Test
    void testMapOnLeft() {
        Either<String, Integer> failure = Either.failure("Error");
        Either<String, String> mapped = failure.map(val -> "Number " + val);

        assertThat(EitherAssert.assertLeft(mapped)).isEqualTo("Error");
    }

    @Test
    void testFlatMapOnRightReturningRight() {
        Either<String, Integer> success = Either.success(10);
        Either<String, Integer> flatMapped = success.flatMap(val -> Either.success(val * 2));

        assertThat(EitherAssert.assertRight(flatMapped)).isEqualTo(20);
    }

    @Test
    void testFlatMapOnRightReturningLeft() {
        Either<String, Integer> success = Either.success(10);
        Either<String, Integer> flatMapped = success.flatMap(val -> Either.failure("Failed later"));

        assertThat(EitherAssert.assertLeft(flatMapped)).isEqualTo("Failed later");
    }

    @Test
    void testFlatMapOnLeft() {
        Either<String, Integer> failure = Either.failure("Initial Error");
        Either<String, Integer> flatMapped = failure.flatMap(val -> Either.success(val * 2));

        assertThat(EitherAssert.assertLeft(flatMapped)).isEqualTo("Initial Error");
    }
}