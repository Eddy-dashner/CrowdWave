package rw.ticketing.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
public class TicketTypeRequest {
    @NotBlank @Size(max = 100) public String name;
    public String description;
    @NotNull @DecimalMin("0.00") public BigDecimal price;
    @NotNull @Min(1) public Integer totalQuantity;
    public LocalDateTime saleStartsAt;
    public LocalDateTime saleEndsAt;
    // Group booking fields
    @Builder.Default public boolean isGroupTicket = false;
    @Min(1) public Integer minGroupSize;
    @Min(1) public Integer maxGroupSize;
}