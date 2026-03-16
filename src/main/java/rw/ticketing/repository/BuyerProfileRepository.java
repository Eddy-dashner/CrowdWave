package rw.ticketing.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.ticketing.entity.BuyerProfile;
import rw.ticketing.entity.User;
import java.util.Optional;
import java.util.UUID;
public interface BuyerProfileRepository extends JpaRepository<BuyerProfile, UUID> {
    Optional<BuyerProfile> findByUser(User user);
    Optional<BuyerProfile> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
