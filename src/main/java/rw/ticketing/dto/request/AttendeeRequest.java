package rw.ticketing.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AttendeeRequest {
    @NotBlank public String firstName;
    @NotBlank public String lastName;
    @NotBlank @Email public String email;
}