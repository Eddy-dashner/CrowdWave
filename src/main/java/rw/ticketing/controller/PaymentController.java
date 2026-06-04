package rw.ticketing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ticketing.dto.request.InitiatePaymentRequest;
import rw.ticketing.dto.response.MessageResponse;
import rw.ticketing.dto.response.PaymentResponse;
import rw.ticketing.entity.User;
import rw.ticketing.service.impl.PaymentService;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Payments", description = "Initiate MTN MoMo / Airtel Money payments and handle callbacks")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Step 1 — buyer calls this to trigger USSD push to their phone.
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.initiatePayment(req, currentUser));
    }

    /**
     * Step 2 — frontend polls this to check if the buyer approved on their phone.
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<PaymentResponse> paymentStatus(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(orderId, currentUser));
    }

    @Operation(summary = "MTN MoMo webhook", description = "Called by MTN servers after payment succeeds or fails. Public — secured by externalRef matching.")
    @SecurityRequirements
    @PutMapping("/callback/momo/{externalRef}")
    public ResponseEntity<MessageResponse> momoCallback(
            @PathVariable String externalRef,
            @RequestBody Map<String, Object> payload) {
        log.info("MoMo callback received for ref: {}", externalRef);
        String status = (String) payload.getOrDefault("status", "FAILED");
        paymentService.handleMomoCallback(externalRef, status, payload);
        return ResponseEntity.ok(new MessageResponse("Callback received"));
    }

    @Operation(summary = "Airtel Money webhook", description = "Called by Airtel servers after payment result.")
    @SecurityRequirements
    @PostMapping("/callback/airtel/{externalRef}")
    public ResponseEntity<MessageResponse> airtelCallback(
            @PathVariable String externalRef,
            @RequestBody Map<String, Object> payload) {
        log.info("Airtel callback received for ref: {}", externalRef);
        String status = (String) payload.getOrDefault("status", "FAILED");
        paymentService.handleMomoCallback(externalRef, status, payload); // same handler, same flow
        return ResponseEntity.ok(new MessageResponse("Callback received"));
    }
}
