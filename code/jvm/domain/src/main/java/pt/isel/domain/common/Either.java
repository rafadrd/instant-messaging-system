package pt.isel.domain.common;

public sealed interface Either<L, R> permits Either.Left, Either.Right {
    static <L, R> Right<L, R> success(R value) {
        return new Right<>(value);
    }

    static <L, R> Left<L, R> failure(L error) {
        return new Left<>(error);
    }

    record Left<L, R>(L value) implements Either<L, R> {
    }

    record Right<L, R>(R value) implements Either<L, R> {
    }
}