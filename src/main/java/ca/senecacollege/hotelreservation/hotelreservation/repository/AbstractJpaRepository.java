package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base JPA implementation of {@link CrudRepository}. Concrete repositories extend this,
 * pass their entity {@code Class} to the constructor, and add their own query methods.
 *
 * <p>Lifecycle rule enforced here: the {@link jakarta.persistence.EntityManagerFactory}
 * is the shared singleton from {@link JpaUtil}, but a brand-new {@link EntityManager} is
 * opened and closed for every single operation — one unit of work, never shared across
 * threads. Write operations run inside a transaction; failures roll back and rethrow.</p>
 *
 * @param <T>  entity type
 * @param <ID> identifier type
 */
public abstract class AbstractJpaRepository<T, ID> implements CrudRepository<T, ID> {

    private final Class<T> entityClass;

    protected AbstractJpaRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /* ---------- unit-of-work helpers ---------- */

    /** Runs {@code work} inside a transaction and returns its result. */
    protected <R> R inTransaction(Function<EntityManager, R> work) {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    /** Runs a write action that returns nothing, inside a transaction. */
    protected void inTransactionVoid(Consumer<EntityManager> work) {
        inTransaction(em -> {
            work.accept(em);
            return null;
        });
    }

    /** Runs a read-only action on a fresh EntityManager (no transaction needed). */
    protected <R> R read(Function<EntityManager, R> work) {
        EntityManager em = JpaUtil.createEntityManager();
        try {
            return work.apply(em);
        } finally {
            em.close();
        }
    }

    /* ---------- CrudRepository ---------- */

    @Override
    public T save(T entity) {
        return inTransaction(em -> {
            em.persist(entity);
            return entity;
        });
    }

    @Override
    public T update(T entity) {
        return inTransaction(em -> em.merge(entity));
    }

    @Override
    public Optional<T> findById(ID id) {
        return read(em -> Optional.ofNullable(em.find(entityClass, id)));
    }

    @Override
    public List<T> findAll() {
        return read(em -> em.createQuery(
                "select e from " + entityClass.getSimpleName() + " e", entityClass).getResultList());
    }

    @Override
    public void delete(T entity) {
        inTransactionVoid(em -> em.remove(em.contains(entity) ? entity : em.merge(entity)));
    }

    @Override
    public void deleteById(ID id) {
        inTransactionVoid(em -> {
            T managed = em.find(entityClass, id);
            if (managed != null) {
                em.remove(managed);
            }
        });
    }

    @Override
    public long count() {
        return read(em -> em.createQuery(
                "select count(e) from " + entityClass.getSimpleName() + " e", Long.class).getSingleResult());
    }
}
