package rw.ticketing.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;
@Data
public class OrderItemRequest {
    @NotNull public UUID ticketTypeId;
    @NotNull @Min(1) @Max(10) public Integer quantity;
}
