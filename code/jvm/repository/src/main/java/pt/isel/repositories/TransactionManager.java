package pt.isel.repositories;

import java.util.function.Function;

public interface TransactionManager {
    <R> R run(Function<Transaction, R> block);
}