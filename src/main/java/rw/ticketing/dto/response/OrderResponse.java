package rw.ticketing.dto.response;
import lombok.*;
import rw.ticketing.enums.OrderStatus;
import rw.ticketing.enums.PaymentProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Data @Builder
public class OrderResponse {
    private UUID id;
    private UUID eventId;
    private String eventTitle;
    private BigDecimal totalAmount;
    private String currency;
    private OrderStatus status;
    private PaymentProvider paymentMethod;
    private List<OrderItemResponse> items;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    @Data @Builder
    public static class OrderItemResponse {
        private UUID id;
        private String ticketTypeName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
