package rw.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "RWF";

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "sold_quantity", nullable = false)
    @Builder.Default
    private Integer soldQuantity = 0;

    @Column(name = "sale_starts_at")
    private LocalDateTime saleStartsAt;

    @Column(name = "sale_ends_at")
    private LocalDateTime saleEndsAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
    @Column(name = "is_group_ticket", nullable = false)
    @Builder.Default
    private boolean isGroupTicket = false;

    @Column(name = "min_group_size", nullable = false)
    @Builder.Default
    private Integer minGroupSize = 1;

    @Column(name = "max_group_size", nullable = false)
    @Builder.Default
    private Integer maxGroupSize = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public int getAvailableQuantity() {
        return totalQuantity - soldQuantity;
    }

    public boolean hasAvailability(int requested) {
        return getAvailableQuantity() >= requested;
    }

    public boolean isSaleOpen() {
        LocalDateTime now = LocalDateTime.now();
        if (!isActive) return false;
        if (saleStartsAt != null && now.isBefore(saleStartsAt)) return false;
        if (saleEndsAt != null && now.isAfter(saleEndsAt)) return false;
        return true;
    }

    public synchronized void incrementSold(int qty) {
        this.soldQuantity += qty;
    }

    public synchronized void decrementSold(int qty) {
        this.soldQuantity = Math.max(0, this.soldQuantity - qty);
    }
}
