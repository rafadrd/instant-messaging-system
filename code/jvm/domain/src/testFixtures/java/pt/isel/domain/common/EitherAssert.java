package pt.isel.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

public class EitherAssert {

    public static <L, R> R assertRight(Either<L, R> either) {
        assertThat(either)
                .as("Expected Either to be Right but was Left")
                .isInstanceOf(Either.Right.class);
        return ((Either.Right<L, R>) either).value();
    }

    public static <L, R> void assertLeft(Either<L, R> either, Class<? extends L> expectedErrorClass) {
        assertThat(either)
                .as("Expected Either to be Left but was Right")
                .isInstanceOf(Either.Left.class);
        L error = ((Either.Left<L, R>) either).value();
        assertThat(error).isInstanceOf(expectedErrorClass);
    }

    public static <L, R> L assertLeft(Either<L, R> either) {
        assertThat(either)
                .as("Expected Either to be Left but was Right")
                .isInstanceOf(Either.Left.class);
        return ((Either.Left<L, R>) either).value();
    }
}