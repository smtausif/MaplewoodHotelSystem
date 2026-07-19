package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Discount;

import java.util.List;

/**
 * Data access for {@link Discount} (discounts applied to a bill, for audit).
 */
public class DiscountRepository extends AbstractJpaRepository<Discount, Long> {

    public DiscountRepository() {
        super(Discount.class);
    }

    public List<Discount> findByBillingId(Long billingId) {
        return read(em -> em.createQuery(
                        "select d from Discount d where d.billing.id = :bid order by d.createdAt", Discount.class)
                .setParameter("bid", billingId)
                .getResultList());
    }
}
