package pt.isel.repositories

/** Generic repository interface for managing transactions */
interface TransactionManager {
    /**
     * This method creates an instance of pt.isel.repositories.Transaction, potentially initializing a JDBI Handle, which
     * is then passed as an argument to the pt.isel.repositories.Transaction constructor.
     */
    fun <R> run(block: Transaction.() -> R): R
}
