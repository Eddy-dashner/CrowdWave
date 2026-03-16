package rw.ticketing.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import rw.ticketing.enums.EventCategory;
import java.time.LocalDateTime;
import java.util.List;
@Data
public class CreateEventRequest {
    @NotBlank @Size(max=255) public String title;
    public String description;
    @NotNull public EventCategory category;
    @NotBlank public String venueName;
    public String venueAddress;
    @NotBlank public String city;
    public String bannerUrl;
    @NotNull @Future(message="Start time must be in the future") public LocalDateTime startsAt;
    @NotNull public LocalDateTime endsAt;
    @NotEmpty @Valid public List<TicketTypeRequest> ticketTypes;
}
