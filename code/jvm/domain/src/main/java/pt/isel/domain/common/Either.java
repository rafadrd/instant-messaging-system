package pt.isel.domain.common;

import java.util.function.Function;

public sealed interface Either<L, R> permits Either.Left, Either.Right {
    static <L, R> Right<L, R> success(R value) {
        return new Right<>(value);
    }

    static <L, R> Left<L, R> failure(L error) {
        return new Left<>(error);
    }

    default <T> Either<L, T> map(Function<R, T> mapper) {
        return switch (this) {
            case Right<L, R>(var value) -> success(mapper.apply(value));
            case Left<L, R>(var error) -> failure(error);
        };
    }

    default <T> Either<L, T> flatMap(Function<R, Either<L, T>> mapper) {
        return switch (this) {
            case Right<L, R>(var value) -> mapper.apply(value);
            case Left<L, R>(var error) -> failure(error);
        };
    }

    record Left<L, R>(L value) implements Either<L, R> {
    }

    record Right<L, R>(R value) implements Either<L, R> {
    }
}