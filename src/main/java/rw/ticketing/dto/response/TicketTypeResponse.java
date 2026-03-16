package rw.ticketing.dto.response;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder
public class TicketTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer totalQuantity;
    private Integer soldQuantity;
    private Integer availableQuantity;
    private LocalDateTime saleStartsAt;
    private LocalDateTime saleEndsAt;
    private boolean isActive;
    private boolean isSaleOpen;
}
