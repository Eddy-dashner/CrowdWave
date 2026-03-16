package rw.ticketing.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import rw.ticketing.enums.Role;
@Data
public class RegisterRequest {
    @NotBlank @Size(min=2, max=100) public String firstName;
    @NotBlank @Size(min=2, max=100) public String lastName;
    @NotBlank @Email public String email;
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number") public String phone;
    @NotBlank @Size(min=8, message="Password must be at least 8 characters") public String password;
    @NotNull public Role role; // BUYER or ORGANIZER
    // Required only when role = ORGANIZER
    public String businessName;
}
