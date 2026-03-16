package rw.ticketing.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ticketing.entity.BuyerProfile;
import rw.ticketing.entity.Ticket;
import java.util.Optional;
import java.util.UUID;
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Page<Ticket> findByBuyerOrderByIssuedAtDesc(BuyerProfile buyer, Pageable pageable);
    Optional<Ticket> findByTicketCode(String ticketCode);
    Page<Ticket> findByEventId(UUID eventId, Pageable pageable);
    long countByEventId(UUID eventId);
}
