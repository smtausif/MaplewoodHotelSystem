package ca.senecacollege.hotelreservation.hotelreservation.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Singleton holder for the JPA {@link EntityManagerFactory}.
 *
 * <p>The spec requires the {@code EntityManagerFactory} to be created once and treated
 * as a singleton, while each {@link EntityManager} is created per unit of work and
 * never shared across threads. This class enforces exactly that: one heavyweight
 * factory for the whole application, and a fresh short-lived {@code EntityManager}
 * on every {@link #createEntityManager()} call.</p>
 */
public final class JpaUtil {

    /** Must match the persistence-unit name in {@code META-INF/persistence.xml}. */
    private static final String PERSISTENCE_UNIT = "maplewoodPU";

    /**
     * The one and only factory. Built lazily on first use and held for the lifetime
     * of the JVM. Initialization-on-demand via a static field keeps it thread-safe.
     */
    private static volatile EntityManagerFactory factory;

    private JpaUtil() {
    }

    /** Returns the shared, application-wide {@link EntityManagerFactory}. */
    public static EntityManagerFactory getEntityManagerFactory() {
        EntityManagerFactory result = factory;
        if (result == null) {
            synchronized (JpaUtil.class) {
                result = factory;
                if (result == null) {
                    result = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
                    factory = result;
                }
            }
        }
        return result;
    }

    /**
     * Creates a new, per-unit-of-work {@link EntityManager}. Callers own it and must
     * close it when done (repositories do this for every operation).
     */
    public static EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /** Closes the factory (release DB connections). Call once on application shutdown. */
    public static synchronized void shutdown() {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
        factory = null;
    }
}
