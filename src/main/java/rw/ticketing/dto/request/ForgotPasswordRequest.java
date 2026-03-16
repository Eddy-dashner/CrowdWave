package rw.ticketing.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ForgotPasswordRequest {
    @NotBlank @Email public String email;
}
