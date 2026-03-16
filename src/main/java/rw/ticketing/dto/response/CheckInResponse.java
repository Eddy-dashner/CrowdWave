package rw.ticketing.dto.response;
import lombok.*;
import rw.ticketing.enums.TicketStatus;
@Data @Builder
public class CheckInResponse {
    private boolean success;
    private String ticketCode;
    private String attendeeName;
    private String eventTitle;
    private String ticketTypeName;
    private TicketStatus previousStatus;
    private String message;
}
