package rw.ticketing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ticketing.dto.request.CreateEventRequest;
import rw.ticketing.dto.response.EventResponse;
import rw.ticketing.entity.User;
import rw.ticketing.service.impl.EventService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ── Public endpoints ────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<EventResponse>> listEvents(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "startsAt") Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(eventService.searchEvents(q, pageable));
        }
        return ResponseEntity.ok(eventService.listPublicEvents(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    // ── Organizer endpoints ─────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(req, currentUser));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<EventResponse>> myEvents(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getMyEvents(currentUser, pageable));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publishEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(eventService.publishEvent(id, currentUser));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(eventService.cancelEvent(id, currentUser));
    }
}
