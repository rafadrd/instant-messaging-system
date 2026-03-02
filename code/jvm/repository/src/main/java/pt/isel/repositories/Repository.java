package pt.isel.repositories;

import java.util.List;

public interface Repository<T> {
    T findById(Long id);

    List<T> findAll();

    void save(T entity);

    void deleteById(Long id);

    void clear();
}