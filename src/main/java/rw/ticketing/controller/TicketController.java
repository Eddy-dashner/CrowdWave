package rw.ticketing.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ticketing.dto.response.CheckInResponse;
import rw.ticketing.dto.response.TicketResponse;
import rw.ticketing.entity.User;
import rw.ticketing.service.impl.OrderService;

import java.util.UUID;

@Tag(name = "Tickets", description = "View tickets and check in attendees at the gate (ORGANIZER/ADMIN only for check-in)")
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> myTickets(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyTickets(currentUser, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(orderService.getTicket(id, currentUser));
    }

    /**
     * Gate scanner — used by organizer staff at the event entrance.
     * Requires ORGANIZER or ADMIN role.
     */
    @PostMapping("/check-in/{code}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<CheckInResponse> checkIn(
            @PathVariable String code,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(orderService.checkIn(code, currentUser));
    }
}
