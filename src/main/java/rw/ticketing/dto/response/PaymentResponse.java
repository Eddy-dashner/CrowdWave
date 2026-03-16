package rw.ticketing.dto.response;
import lombok.*;
import rw.ticketing.enums.PaymentProvider;
import rw.ticketing.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private PaymentProvider provider;
    private PaymentStatus status;
    private String message;
    private LocalDateTime initiatedAt;
}
