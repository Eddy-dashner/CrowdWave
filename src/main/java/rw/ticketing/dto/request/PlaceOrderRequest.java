package rw.ticketing.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.UUID;
@Data
public class PlaceOrderRequest {
    @NotNull public UUID eventId;
    @NotEmpty @Valid public List<OrderItemRequest> items;
}
