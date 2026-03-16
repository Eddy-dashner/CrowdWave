package rw.ticketing.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ticketing.entity.TicketType;
import java.util.List;
import java.util.UUID;
public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
    List<TicketType> findByEventIdAndIsActiveTrue(UUID eventId);
}
