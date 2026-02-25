package pt.isel.repositories

/** Generic repository interface for basic CRUD operations */
interface Repository<T> {
    fun findById(id: Long): T?

    fun findAll(): List<T>

    fun save(entity: T)

    fun deleteById(id: Long)

    fun clear()
}
