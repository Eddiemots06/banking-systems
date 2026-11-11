package com.bac.banking.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object contract: the four CRUD operations every DAO provides.
 *
 * @param <T> the domain type stored
 * @param <K> the type of its primary key
 */
public interface DAO<T, K> {

    /** CREATE: inserts the object and returns it (with any generated key set). */
    T create(T entity);

    /** READ one by primary key. */
    Optional<T> findById(K id);

    /** READ all. */
    List<T> findAll();

    /** UPDATE: returns true if a row was changed. */
    boolean update(T entity);

    /** DELETE by primary key: returns true if a row was removed. */
    boolean delete(K id);
}
