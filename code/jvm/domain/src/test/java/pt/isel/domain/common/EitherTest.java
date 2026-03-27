package pt.isel.domain.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EitherTest {

    @Test
    void testSuccessCreation() {
        Either<String, Integer> result = Either.success(42);

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<String, Integer>) result).value()).isEqualTo(42);
    }

    @Test
    void testFailureCreation() {
        Either<String, Integer> result = Either.failure("Error occurred");

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<String, Integer>) result).value()).isEqualTo("Error occurred");
    }

    @Test
    void testMapOnRight() {
        Either<String, Integer> success = Either.success(10);
        Either<String, String> mapped = success.map(val -> "Number " + val);

        assertThat(mapped).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<String, String>) mapped).value()).isEqualTo("Number 10");
    }

    @Test
    void testMapOnLeft() {
        Either<String, Integer> failure = Either.failure("Error");
        Either<String, String> mapped = failure.map(val -> "Number " + val);

        assertThat(mapped).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<String, String>) mapped).value()).isEqualTo("Error");
    }

    @Test
    void testFlatMapOnRightReturningRight() {
        Either<String, Integer> success = Either.success(10);
        Either<String, Integer> flatMapped = success.flatMap(val -> Either.success(val * 2));

        assertThat(flatMapped).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<String, Integer>) flatMapped).value()).isEqualTo(20);
    }

    @Test
    void testFlatMapOnRightReturningLeft() {
        Either<String, Integer> success = Either.success(10);
        Either<String, Integer> flatMapped = success.flatMap(val -> Either.failure("Failed later"));

        assertThat(flatMapped).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<String, Integer>) flatMapped).value()).isEqualTo("Failed later");
    }

    @Test
    void testFlatMapOnLeft() {
        Either<String, Integer> failure = Either.failure("Initial Error");
        Either<String, Integer> flatMapped = failure.flatMap(val -> Either.success(val * 2));

        assertThat(flatMapped).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<String, Integer>) flatMapped).value()).isEqualTo("Initial Error");
    }
}