package rw.ticketing.dto.response;
import lombok.*;
import rw.ticketing.enums.Role;
import java.util.UUID;
@Data @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UUID userId;
    private String email;
    private String fullName;
    private Role role;
    private boolean hasOrganizerProfile;
    private boolean hasBuyerProfile;
}
