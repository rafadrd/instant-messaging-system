package pt.isel.repositories.jdbi.transaction

import org.jdbi.v3.core.Jdbi
import pt.isel.repositories.Transaction
import pt.isel.repositories.TransactionManager

class TransactionManagerJdbi(
    private val jdbi: Jdbi,
) : TransactionManager {
    override fun <R> run(block: (Transaction) -> R): R =
        jdbi.inTransaction<R, Exception> { handle ->
            val transaction = TransactionJdbi(handle)
            block(transaction)
        }
}
