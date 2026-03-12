package pt.isel.repositories.jdbi.transaction;

import org.jdbi.v3.core.Jdbi;
import pt.isel.domain.common.Either;
import pt.isel.repositories.Transaction;
import pt.isel.repositories.TransactionManager;

import java.util.function.Function;

public class TransactionManagerJdbi implements TransactionManager {
    private final Jdbi jdbi;

    public TransactionManagerJdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public <R> R run(Function<Transaction, R> block) {
        try {
            return jdbi.inTransaction((handle) -> {
                Transaction transaction = new TransactionJdbi(handle);
                R result = block.apply(transaction);

                if (result instanceof Either.Left<?, ?> left) {
                    throw new RollbackException(left);
                }
                return result;
            });
        } catch (RollbackException e) {
            @SuppressWarnings("unchecked")
            R result = (R) e.getLeft();
            return result;
        }
    }

    private static class RollbackException extends RuntimeException {
        private final Either.Left<?, ?> left;

        public RollbackException(Either.Left<?, ?> left) {
            this.left = left;
        }

        public Either.Left<?, ?> getLeft() {
            return left;
        }
    }
}