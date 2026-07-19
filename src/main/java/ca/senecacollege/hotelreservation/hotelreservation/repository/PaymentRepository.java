package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Payment;

import java.util.List;

/**
 * Data access for {@link Payment} (the per-bill payment/refund ledger).
 */
public class PaymentRepository extends AbstractJpaRepository<Payment, Long> {

    public PaymentRepository() {
        super(Payment.class);
    }

    /** All payments against a bill, oldest first (ledger order). */
    public List<Payment> findByBillingId(Long billingId) {
        return read(em -> em.createQuery(
                        "select p from Payment p where p.billing.id = :bid order by p.createdAt", Payment.class)
                .setParameter("bid", billingId)
                .getResultList());
    }
}
