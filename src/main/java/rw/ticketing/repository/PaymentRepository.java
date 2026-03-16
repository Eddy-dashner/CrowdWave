package rw.ticketing.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ticketing.entity.Payment;
import java.util.Optional;
import java.util.UUID;
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByProviderReference(String ref);
    Optional<Payment> findTopByOrderIdOrderByInitiatedAtDesc(UUID orderId);
}
