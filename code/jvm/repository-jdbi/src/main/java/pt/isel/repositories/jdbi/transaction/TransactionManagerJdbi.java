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
        return jdbi.inTransaction((handle) -> {
            try {
                Transaction transaction = new TransactionJdbi(handle);
                R result = block.apply(transaction);

                if (result instanceof Either.Left<?, ?>) {
                    // Manually roll back on business errors to prevent JDBI from committing the transaction.
                    handle.rollback();
                }

                return result;
            } catch (Exception e) {
                handle.rollback();
                throw e;
            }
        });
    }
}