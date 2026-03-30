package rw.ticketing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ticketing.entity.TicketAttendee;
import java.util.List;
import java.util.UUID;

public interface TicketAttendeeRepository extends JpaRepository<TicketAttendee, UUID> {
    List<TicketAttendee> findByTicketId(UUID ticketId);
}
