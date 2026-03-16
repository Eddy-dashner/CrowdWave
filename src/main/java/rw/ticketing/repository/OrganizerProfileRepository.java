package rw.ticketing.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ticketing.entity.OrganizerProfile;
import rw.ticketing.entity.User;
import java.util.Optional;
import java.util.UUID;
public interface OrganizerProfileRepository extends JpaRepository<OrganizerProfile, UUID> {
    Optional<OrganizerProfile> findByUser(User user);
    Optional<OrganizerProfile> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
