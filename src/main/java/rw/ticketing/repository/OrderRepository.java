package rw.ticketing.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ticketing.entity.BuyerProfile;
import rw.ticketing.entity.Order;
import rw.ticketing.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByBuyerOrderByCreatedAtDesc(BuyerProfile buyer, Pageable pageable);
    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime now);
}
