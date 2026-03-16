package rw.ticketing.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class RefreshTokenRequest {
    @NotBlank public String refreshToken;
}
