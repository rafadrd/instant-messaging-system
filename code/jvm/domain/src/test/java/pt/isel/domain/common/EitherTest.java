package pt.isel.domain.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EitherTest {

    @Test
    void testSuccessCreation() {
        Either<String, Integer> result = Either.success(42);

        assertInstanceOf(Either.Right.class, result);
        assertEquals(42, ((Either.Right<String, Integer>) result).value());
    }

    @Test
    void testFailureCreation() {
        Either<String, Integer> result = Either.failure("Error occurred");

        assertInstanceOf(Either.Left.class, result);
        assertEquals("Error occurred", ((Either.Left<String, Integer>) result).value());
    }

    @Test
    void testMapOnRight() {
        Either<String, Integer> success = Either.success(10);
        Either<String, String> mapped = success.map(val -> "Number " + val);

        assertInstanceOf(Either.Right.class, mapped);
        assertEquals("Number 10", ((Either.Right<String, String>) mapped).value());
    }

    @Test
    void testMapOnLeft() {
        Either<String, Integer> failure = Either.failure("Error");
        Either<String, String> mapped = failure.map(val -> "Number " + val);

        assertInstanceOf(Either.Left.class, mapped);
        assertEquals("Error", ((Either.Left<String, String>) mapped).value());
    }

    @Test
    void testFlatMapOnRightReturningRight() {
        Either<String, Integer> success = Either.success(10);
        Either<String, Integer> flatMapped = success.flatMap(val -> Either.success(val * 2));

        assertInstanceOf(Either.Right.class, flatMapped);
        assertEquals(20, ((Either.Right<String, Integer>) flatMapped).value());
    }

    @Test
    void testFlatMapOnRightReturningLeft() {
        Either<String, Integer> success = Either.success(10);
        Either<String, Integer> flatMapped = success.flatMap(val -> Either.failure("Failed later"));

        assertInstanceOf(Either.Left.class, flatMapped);
        assertEquals("Failed later", ((Either.Left<String, Integer>) flatMapped).value());
    }

    @Test
    void testFlatMapOnLeft() {
        Either<String, Integer> failure = Either.failure("Initial Error");
        Either<String, Integer> flatMapped = failure.flatMap(val -> Either.success(val * 2));

        assertInstanceOf(Either.Left.class, flatMapped);
        assertEquals("Initial Error", ((Either.Left<String, Integer>) flatMapped).value());
    }
}