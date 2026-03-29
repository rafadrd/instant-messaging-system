package pt.isel.domain.common;

import org.junit.jupiter.api.Test;

class EitherTest {

    @Test
    void Success_ValidValue_ReturnsRight() {
        Either<String, Integer> result = Either.success(42);

        EitherAssert.assertThat(result).containsRight(42);
    }

    @Test
    void Failure_ValidError_ReturnsLeft() {
        Either<String, Integer> result = Either.failure("Error occurred");

        EitherAssert.assertThat(result).containsLeft("Error occurred");
    }

    @Test
    void Map_OnRight_TransformsValue() {
        Either<String, Integer> success = Either.success(10);

        Either<String, String> mapped = success.map(val -> "Number " + val);

        EitherAssert.assertThat(mapped).containsRight("Number 10");
    }

    @Test
    void Map_OnLeft_ReturnsLeft() {
        Either<String, Integer> failure = Either.failure("Error");

        Either<String, String> mapped = failure.map(val -> "Number " + val);

        EitherAssert.assertThat(mapped).containsLeft("Error");
    }

    @Test
    void FlatMap_OnRightReturningRight_ReturnsRight() {
        Either<String, Integer> success = Either.success(10);

        Either<String, Integer> flatMapped = success.flatMap(val -> Either.success(val * 2));

        EitherAssert.assertThat(flatMapped).containsRight(20);
    }

    @Test
    void FlatMap_OnRightReturningLeft_ReturnsLeft() {
        Either<String, Integer> success = Either.success(10);

        Either<String, Integer> flatMapped = success.flatMap(val -> Either.failure("Failed later"));

        EitherAssert.assertThat(flatMapped).containsLeft("Failed later");
    }

    @Test
    void FlatMap_OnLeft_ReturnsLeft() {
        Either<String, Integer> failure = Either.failure("Initial Error");

        Either<String, Integer> flatMapped = failure.flatMap(val -> Either.success(val * 2));

        EitherAssert.assertThat(flatMapped).containsLeft("Initial Error");
    }
}