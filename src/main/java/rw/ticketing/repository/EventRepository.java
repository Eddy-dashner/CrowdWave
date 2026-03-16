package rw.ticketing.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rw.ticketing.entity.Event;
import rw.ticketing.entity.OrganizerProfile;
import rw.ticketing.enums.EventCategory;
import java.time.LocalDateTime;
import java.util.UUID;
public interface EventRepository extends JpaRepository<Event, UUID> {
    Page<Event> findByIsPublishedTrueAndStartsAtAfterOrderByStartsAtAsc(LocalDateTime now, Pageable pageable);
    Page<Event> findByOrganizerOrderByCreatedAtDesc(OrganizerProfile organizer, Pageable pageable);
    Page<Event> findByIsPublishedTrueAndCategoryAndStartsAtAfter(EventCategory category, LocalDateTime now, Pageable pageable);
    @Query("SELECT e FROM Event e WHERE e.isPublished = true AND e.startsAt > :now " +
           "AND (LOWER(e.title) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(e.venueName) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Event> searchPublished(String q, LocalDateTime now, Pageable pageable);
}
