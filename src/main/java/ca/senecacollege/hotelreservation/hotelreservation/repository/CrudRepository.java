package ca.senecacollege.hotelreservation.hotelreservation.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic data-access contract shared by every repository (Repository pattern).
 *
 * <p>Following the course material: the interface defines the contract, while a JPA
 * implementation ({@link AbstractJpaRepository}) manages the actual data. Services
 * depend on this abstraction, never on the ORM API directly, so the storage
 * (memory, database, API) can be swapped without touching business code.</p>
 *
 * @param <T>  the entity type
 * @param <ID> the entity's identifier type
 */
public interface CrudRepository<T, ID> {

    /** Inserts a new entity and returns the managed instance (with its generated id). */
    T save(T entity);

    /** Updates an existing entity and returns the merged instance. */
    T update(T entity);

    /** Finds an entity by primary key. */
    Optional<T> findById(ID id);

    /** Returns every entity of this type. */
    List<T> findAll();

    /** Deletes the given entity. */
    void delete(T entity);

    /** Deletes the entity with the given primary key, if it exists. */
    void deleteById(ID id);

    /** Counts all entities of this type. */
    long count();
}
