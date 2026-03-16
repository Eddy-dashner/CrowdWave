package rw.ticketing.controller;

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

    /**
     * MTN MoMo webhook — called by MTN servers after payment succeeds or fails.
     * This endpoint is public (no JWT) — secured by matching the externalRef.
     */
    @PutMapping("/callback/momo/{externalRef}")
    public ResponseEntity<MessageResponse> momoCallback(
            @PathVariable String externalRef,
            @RequestBody Map<String, Object> payload) {
        log.info("MoMo callback received for ref: {}", externalRef);
        String status = (String) payload.getOrDefault("status", "FAILED");
        paymentService.handleMomoCallback(externalRef, status, payload);
        return ResponseEntity.ok(new MessageResponse("Callback received"));
    }

    /**
     * Airtel Money webhook — same pattern as MoMo.
     */
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
