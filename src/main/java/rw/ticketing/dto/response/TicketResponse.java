package rw.ticketing.dto.response;
import lombok.*;
import rw.ticketing.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder
public class TicketResponse {
    private UUID id;
    private String ticketCode;
    private String qrCodeBase64;
    private TicketStatus status;
    private String eventTitle;
    private String eventVenue;
    private LocalDateTime eventStartsAt;
    private String ticketTypeName;
    private LocalDateTime issuedAt;
    private LocalDateTime checkedInAt;
}
