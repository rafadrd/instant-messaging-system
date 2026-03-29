package pt.isel.domain.common;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class EitherAssert<L, R> extends AbstractAssert<EitherAssert<L, R>, Either<L, R>> {

    public EitherAssert(Either<L, R> actual) {
        super(actual, EitherAssert.class);
    }

    public static <L, R> EitherAssert<L, R> assertThat(Either<L, R> actual) {
        return new EitherAssert<>(actual);
    }

    public EitherAssert<L, R> isRight() {
        isNotNull();
        if (!(actual instanceof Either.Right)) {
            failWithMessage("Expected Either to be Right but was Left: %s", ((Either.Left<L, R>) actual).value());
        }
        return this;
    }

    public EitherAssert<L, R> isLeft() {
        isNotNull();
        if (!(actual instanceof Either.Left)) {
            failWithMessage("Expected Either to be Left but was Right: %s", ((Either.Right<L, R>) actual).value());
        }
        return this;
    }

    public EitherAssert<L, R> containsRight(R expected) {
        isRight();
        Assertions.assertThat(((Either.Right<L, R>) actual).value()).isEqualTo(expected);
        return this;
    }

    public EitherAssert<L, R> containsLeft(L expected) {
        isLeft();
        Assertions.assertThat(((Either.Left<L, R>) actual).value()).isEqualTo(expected);
        return this;
    }

    public R getRightValue() {
        isRight();
        return ((Either.Right<L, R>) actual).value();
    }

    public L getLeftValue() {
        isLeft();
        return ((Either.Left<L, R>) actual).value();
    }

    public EitherAssert<L, R> isLeftInstanceOf(Class<? extends L> expectedClass) {
        isLeft();
        Assertions.assertThat(((Either.Left<L, R>) actual).value()).isInstanceOf(expectedClass);
        return this;
    }
}