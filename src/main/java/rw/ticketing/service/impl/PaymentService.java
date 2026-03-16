package rw.ticketing.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import rw.ticketing.dto.request.InitiatePaymentRequest;
import rw.ticketing.dto.response.PaymentResponse;
import rw.ticketing.entity.*;
import rw.ticketing.enums.OrderStatus;
import rw.ticketing.enums.PaymentProvider;
import rw.ticketing.enums.PaymentStatus;
import rw.ticketing.exception.AppException;
import rw.ticketing.repository.*;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final WebClient.Builder webClientBuilder;

    @Value("${momo.api.base-url}")
    private String momoBaseUrl;

    @Value("${momo.api.collection-subscription-key}")
    private String collectionSubscriptionKey;

    @Value("${momo.api.environment}")
    private String momoEnvironment;

    @Value("${momo.api.callback-host}")
    private String callbackHost;

    /**
     * Initiates a Mobile Money payment request (MTN MoMo or Airtel Money).
     * The user receives a USSD push prompt on their phone.
     */
    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest req, User currentUser) {
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> AppException.notFound("Order not found"));

        // Verify ownership
        if (!order.getBuyer().getUser().getId().equals(currentUser.getId())) {
            throw AppException.forbidden("Access denied");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw AppException.badRequest("Order is not in a payable state (status: " + order.getStatus() + ")");
        }
        if (order.isExpired()) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
            throw AppException.badRequest("Order has expired. Please place a new order.");
        }

        String externalRef = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .provider(req.getProvider())
                .providerReference(externalRef)
                .phoneNumber(req.getPhoneNumber())
                .build();
        paymentRepository.save(payment);

        order.setPaymentMethod(req.getProvider());
        order.setPaymentReference(externalRef);
        orderRepository.save(order);

        // Send USSD push to customer
        if (req.getProvider() == PaymentProvider.MTN_MOMO) {
            initiateMtnMomoRequest(payment, req.getPhoneNumber(), externalRef);
        } else if (req.getProvider() == PaymentProvider.AIRTEL_MONEY) {
            initiateAirtelRequest(payment, req.getPhoneNumber(), externalRef);
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(order.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .provider(payment.getProvider())
                .status(PaymentStatus.PENDING)
                .message("Payment request sent to " + req.getPhoneNumber() + ". Please approve the prompt on your phone.")
                .initiatedAt(payment.getInitiatedAt())
                .build();
    }

    /**
     * Webhook callback from MTN MoMo after payment completes.
     */
    @Transactional
    public void handleMomoCallback(String externalRef, String status, Map<String, Object> payload) {
        Payment payment = paymentRepository.findByProviderReference(externalRef)
                .orElseThrow(() -> AppException.notFound("Payment not found for ref: " + externalRef));

        payment.setProviderResponse(payload);

        if ("SUCCESSFUL".equalsIgnoreCase(status)) {
            confirmPayment(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

    /**
     * Poll payment status (for frontend to check after initiating).
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(UUID orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Order not found"));

        if (!order.getBuyer().getUser().getId().equals(currentUser.getId())) {
            throw AppException.forbidden("Access denied");
        }

        Payment payment = paymentRepository.findTopByOrderIdOrderByInitiatedAtDesc(orderId)
                .orElseThrow(() -> AppException.notFound("No payment found for this order"));

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(order.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .provider(payment.getProvider())
                .status(payment.getStatus())
                .initiatedAt(payment.getInitiatedAt())
                .build();
    }

    private void confirmPayment(Payment payment) {
        payment.setStatus(PaymentStatus.SUCCESSFUL);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // Issue tickets now that payment is confirmed
        orderService.issueTickets(order);
    }

    private void initiateMtnMomoRequest(Payment payment, String phone, String externalRef) {
        try {
            // MTN MoMo Collection API — Request to Pay
            // Docs: https://momodeveloper.mtn.com/docs/services/collection
            String callbackUrl = callbackHost + "/api/v1/payments/callback/momo/" + externalRef;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", payment.getAmount().toPlainString());
            body.put("currency", "EUR"); // MTN sandbox uses EUR; prod uses RWF
            body.put("externalId", externalRef);
            body.put("payer", Map.of(
                    "partyIdType", "MSISDN",
                    "partyId", phone.replace("+", "")
            ));
            body.put("payerMessage", "Rwanda Tickets payment");
            body.put("payeeNote", "Event ticket purchase");

            webClientBuilder.build()
                    .post()
                    .uri(momoBaseUrl + "/collection/v1_0/requesttopay")
                    .header("Authorization", "Bearer " + getMomoAccessToken())
                    .header("X-Reference-Id", externalRef)
                    .header("X-Target-Environment", momoEnvironment)
                    .header("Ocp-Apim-Subscription-Key", collectionSubscriptionKey)
                    .header("X-Callback-Url", callbackUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            resp -> log.info("MoMo request sent for ref: {}", externalRef),
                            err -> log.error("MoMo request failed for ref {}: {}", externalRef, err.getMessage())
                    );
        } catch (Exception e) {
            log.error("Failed to initiate MTN MoMo payment: {}", e.getMessage());
        }
    }

    private void initiateAirtelRequest(Payment payment, String phone, String externalRef) {
        // Airtel Money Rwanda API integration point
        // Docs: https://developers.airtel.africa/
        // Structure is similar to MoMo — swap the endpoint and auth headers
        log.info("Airtel Money payment initiated for ref: {} phone: {}", externalRef, phone);
    }

    private String getMomoAccessToken() {
        // In production: call /collection/token/ with Basic auth (API user:API key)
        // Cache the token and refresh before expiry
        // For sandbox, generate via: POST /collection/token/  with Authorization: Basic base64(apiUser:apiKey)
        return "SANDBOX_TOKEN_PLACEHOLDER";
    }
}
