package rw.ticketing.dto.response;
import lombok.*;
import rw.ticketing.enums.EventCategory;
import rw.ticketing.enums.EventStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Data @Builder
public class EventResponse {
    private UUID id;
    private String title;
    private String description;
    private EventCategory category;
    private String venueName;
    private String venueAddress;
    private String city;
    private String bannerUrl;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private EventStatus status;
    private boolean isPublished;
    private OrganizerSummary organizer;
    private List<TicketTypeResponse> ticketTypes;
    private LocalDateTime createdAt;

    @Data @Builder
    public static class OrganizerSummary {
        private UUID id;
        private String businessName;
        private String logoUrl;
        private boolean isVerified;
    }
}
