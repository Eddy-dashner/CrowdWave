package rw.ticketing.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class LoginRequest {
    @NotBlank @Email public String email;
    @NotBlank public String password;
}
