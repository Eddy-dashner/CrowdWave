package rw.ticketing.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ticketing.dto.request.CreateEventRequest;
import rw.ticketing.dto.request.TicketTypeRequest;
import rw.ticketing.dto.response.EventResponse;
import rw.ticketing.dto.response.TicketTypeResponse;
import rw.ticketing.entity.*;
import rw.ticketing.exception.AppException;
import rw.ticketing.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final OrganizerProfileRepository organizerProfileRepository;

    @Transactional
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public EventResponse createEvent(CreateEventRequest req, User currentUser) {
        OrganizerProfile organizer = organizerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> AppException.notFound("Organizer profile not found"));

        if (req.getEndsAt().isBefore(req.getStartsAt())) {
            throw AppException.badRequest("End time must be after start time");
        }

        Event event = Event.builder()
                .organizer(organizer)
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .venueName(req.getVenueName())
                .venueAddress(req.getVenueAddress())
                .city(req.getCity())
                .bannerUrl(req.getBannerUrl())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .build();
        eventRepository.save(event);

        for (TicketTypeRequest ttr : req.getTicketTypes()) {
            TicketType tt = TicketType.builder()
                    .event(event)
                    .name(ttr.getName())
                    .description(ttr.getDescription())
                    .price(ttr.getPrice())
                    .totalQuantity(ttr.getTotalQuantity())
                    .saleStartsAt(ttr.getSaleStartsAt())
                    .saleEndsAt(ttr.getSaleEndsAt())
                    .build();
            ticketTypeRepository.save(tt);
            event.getTicketTypes().add(tt);
        }

        return toResponse(event);
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public EventResponse publishEvent(UUID eventId, User currentUser) {
        Event event = getAndVerifyOwnership(eventId, currentUser);
        if (event.getTicketTypes().isEmpty()) {
            throw AppException.badRequest("Cannot publish event with no ticket types");
        }
        event.publish();
        eventRepository.save(event);
        return toResponse(event);
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public EventResponse cancelEvent(UUID eventId, User currentUser) {
        Event event = getAndVerifyOwnership(eventId, currentUser);
        event.cancel();
        eventRepository.save(event);
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listPublicEvents(Pageable pageable) {
        return eventRepository
                .findByIsPublishedTrueAndStartsAtAfterOrderByStartsAtAsc(LocalDateTime.now(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> searchEvents(String query, Pageable pageable) {
        return eventRepository.searchPublished(query, LocalDateTime.now(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Event not found"));
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public Page<EventResponse> getMyEvents(User currentUser, Pageable pageable) {
        OrganizerProfile organizer = organizerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> AppException.notFound("Organizer profile not found"));
        return eventRepository.findByOrganizerOrderByCreatedAtDesc(organizer, pageable).map(this::toResponse);
    }

    private Event getAndVerifyOwnership(UUID eventId, User currentUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> AppException.notFound("Event not found"));
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = event.getOrganizer().getUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw AppException.forbidden("You don't have permission to modify this event");
        }
        return event;
    }

    public EventResponse toResponse(Event e) {
        return EventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .category(e.getCategory())
                .venueName(e.getVenueName())
                .venueAddress(e.getVenueAddress())
                .city(e.getCity())
                .bannerUrl(e.getBannerUrl())
                .startsAt(e.getStartsAt())
                .endsAt(e.getEndsAt())
                .status(e.getStatus())
                .isPublished(e.isPublished())
                .organizer(EventResponse.OrganizerSummary.builder()
                        .id(e.getOrganizer().getId())
                        .businessName(e.getOrganizer().getBusinessName())
                        .logoUrl(e.getOrganizer().getLogoUrl())
                        .isVerified(e.getOrganizer().isVerified())
                        .build())
                .ticketTypes(e.getTicketTypes().stream().map(this::toTicketTypeResponse).toList())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public TicketTypeResponse toTicketTypeResponse(TicketType tt) {
        return TicketTypeResponse.builder()
                .id(tt.getId())
                .name(tt.getName())
                .description(tt.getDescription())
                .price(tt.getPrice())
                .currency(tt.getCurrency())
                .totalQuantity(tt.getTotalQuantity())
                .soldQuantity(tt.getSoldQuantity())
                .availableQuantity(tt.getAvailableQuantity())
                .saleStartsAt(tt.getSaleStartsAt())
                .saleEndsAt(tt.getSaleEndsAt())
                .isActive(tt.isActive())
                .isSaleOpen(tt.isSaleOpen())
                .build();
    }
}
