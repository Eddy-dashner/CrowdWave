package rw.ticketing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class OrderItemRequest {
    @NotNull public UUID ticketTypeId;
    @NotNull @Min(1) @Max(20) public Integer quantity;
    // Required when ticket type is a group ticket
    @Valid public List<AttendeeRequest> attendees;
}