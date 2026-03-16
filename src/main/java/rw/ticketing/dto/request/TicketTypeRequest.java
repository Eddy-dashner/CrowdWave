package rw.ticketing.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class TicketTypeRequest {
    @NotBlank @Size(max=100) public String name;
    public String description;
    @NotNull @DecimalMin("0.00") public BigDecimal price;
    @NotNull @Min(1) public Integer totalQuantity;
    public LocalDateTime saleStartsAt;
    public LocalDateTime saleEndsAt;
}
