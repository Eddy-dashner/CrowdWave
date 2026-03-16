package rw.ticketing.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import rw.ticketing.enums.PaymentProvider;
import java.util.UUID;
@Data
public class InitiatePaymentRequest {
    @NotNull public UUID orderId;
    @NotNull public PaymentProvider provider;
    @NotBlank @Pattern(regexp="^\\+?250[0-9]{9}$", message="Enter a valid Rwanda phone number e.g. +250781234567")
    public String phoneNumber;
}
