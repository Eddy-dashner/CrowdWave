package rw.ticketing.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ResetPasswordRequest {
    @NotBlank public String token;
    @NotBlank @Size(min=8) public String newPassword;
}
