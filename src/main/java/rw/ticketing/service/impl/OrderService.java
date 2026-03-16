package rw.ticketing.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ticketing.dto.request.PlaceOrderRequest;
import rw.ticketing.dto.response.*;
import rw.ticketing.entity.*;
import rw.ticketing.enums.EventStatus;
import rw.ticketing.enums.OrderStatus;
import rw.ticketing.enums.TicketStatus;
import rw.ticketing.exception.AppException;
import rw.ticketing.repository.*;
import rw.ticketing.util.QrCodeUtil;
import rw.ticketing.util.TicketCodeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final BuyerProfileRepository buyerProfileRepository;
    private final QrCodeUtil qrCodeUtil;
    private final TicketCodeUtil ticketCodeUtil;
    private final EmailService emailService;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest req, User currentUser) {
        BuyerProfile buyer = buyerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> AppException.notFound("Buyer profile not found. Please complete your profile."));

        Event event = eventRepository.findById(req.getEventId())
                .orElseThrow(() -> AppException.notFound("Event not found"));

        if (event.getStatus() != EventStatus.PUBLISHED || !event.isPublished()) {
            throw AppException.badRequest("This event is not available for booking");
        }
        if (event.getStartsAt().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("This event has already started");
        }

        // Validate and lock ticket types
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (var itemReq : req.getItems()) {
            TicketType tt = ticketTypeRepository.findById(itemReq.getTicketTypeId())
                    .orElseThrow(() -> AppException.notFound("Ticket type not found: " + itemReq.getTicketTypeId()));

            if (!tt.getEvent().getId().equals(event.getId())) {
                throw AppException.badRequest("Ticket type does not belong to this event");
            }
            if (!tt.isSaleOpen()) {
                throw AppException.badRequest("Ticket type '" + tt.getName() + "' is not on sale");
            }
            if (!tt.hasAvailability(itemReq.getQuantity())) {
                throw AppException.badRequest("Not enough tickets available for '" + tt.getName() + "'");
            }

            BigDecimal subtotal = tt.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            OrderItem item = OrderItem.builder()
                    .ticketType(tt)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(tt.getPrice())
                    .subtotal(subtotal)
                    .build();
            items.add(item);
            total = total.add(subtotal);

            // Reserve tickets (pessimistic — decrement happens on payment confirmation)
        }

        Order order = Order.builder()
                .buyer(buyer)
                .event(event)
                .totalAmount(total)
                .currency("RWF")
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // 15-min payment window
                .build();
        orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrder(order);
        }
        order.setItems(items);
        orderRepository.save(order);

        return toOrderResponse(order);
    }

    /**
     * Called after successful payment confirmation to issue tickets.
     */
    @Transactional
    public void issueTickets(Order order) {
        if (order.getStatus() != OrderStatus.PAID) {
            throw AppException.badRequest("Order is not in PAID state");
        }

        List<Ticket> tickets = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            TicketType tt = item.getTicketType();
            tt.incrementSold(item.getQuantity());
            ticketTypeRepository.save(tt);

            for (int i = 0; i < item.getQuantity(); i++) {
                String code = generateUniqueCode();
                String qrData = qrCodeUtil.buildTicketQrData(code, order.getEvent().getId().toString());
                String qrBase64 = qrCodeUtil.generateQrCodeBase64(qrData);

                Ticket ticket = Ticket.builder()
                        .orderItem(item)
                        .ticketType(tt)
                        .event(order.getEvent())
                        .buyer(order.getBuyer())
                        .ticketCode(code)
                        .qrCodeData(qrBase64)
                        .build();
                tickets.add(ticket);
            }
        }
        ticketRepository.saveAll(tickets);

        // Send confirmation email with first ticket
        if (!tickets.isEmpty()) {
            Ticket first = tickets.get(0);
            User buyerUser = order.getBuyer().getUser();
            emailService.sendTicketConfirmation(
                    buyerUser.getEmail(),
                    buyerUser.getFirstName(),
                    order.getEvent().getTitle(),
                    first.getTicketCode(),
                    first.getQrCodeData()
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(User currentUser, Pageable pageable) {
        BuyerProfile buyer = buyerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> AppException.notFound("Buyer profile not found"));
        return orderRepository.findByBuyerOrderByCreatedAtDesc(buyer, pageable).map(this::toOrderResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getMyTickets(User currentUser, Pageable pageable) {
        BuyerProfile buyer = buyerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> AppException.notFound("Buyer profile not found"));
        return ticketRepository.findByBuyerOrderByIssuedAtDesc(buyer, pageable).map(this::toTicketResponse);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID ticketId, User currentUser) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> AppException.notFound("Ticket not found"));
        boolean isOwner = ticket.getBuyer().getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        if (!isOwner && !isAdmin) throw AppException.forbidden("Access denied");
        return toTicketResponse(ticket);
    }

    /**
     * Gate scanner endpoint — verify and check in a ticket by code.
     */
    @Transactional
    public CheckInResponse checkIn(String ticketCode, User staffUser) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> AppException.notFound("Ticket not found: " + ticketCode));

        TicketStatus prev = ticket.getStatus();

        if (ticket.getStatus() == TicketStatus.USED) {
            return CheckInResponse.builder()
                    .success(false)
                    .ticketCode(ticketCode)
                    .previousStatus(prev)
                    .message("Ticket already used at " + ticket.getCheckedInAt())
                    .build();
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED || ticket.getStatus() == TicketStatus.EXPIRED) {
            return CheckInResponse.builder()
                    .success(false)
                    .ticketCode(ticketCode)
                    .previousStatus(prev)
                    .message("Ticket is " + ticket.getStatus().name().toLowerCase())
                    .build();
        }

        ticket.checkIn(staffUser);
        ticketRepository.save(ticket);

        User buyer = ticket.getBuyer().getUser();
        return CheckInResponse.builder()
                .success(true)
                .ticketCode(ticketCode)
                .attendeeName(buyer.getFullName())
                .eventTitle(ticket.getEvent().getTitle())
                .ticketTypeName(ticket.getTicketType().getName())
                .previousStatus(prev)
                .message("Check-in successful")
                .build();
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = ticketCodeUtil.generate();
        } while (ticketRepository.findByTicketCode(code).isPresent());
        return code;
    }

    private OrderResponse toOrderResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .eventId(o.getEvent().getId())
                .eventTitle(o.getEvent().getTitle())
                .totalAmount(o.getTotalAmount())
                .currency(o.getCurrency())
                .status(o.getStatus())
                .paymentMethod(o.getPaymentMethod())
                .expiresAt(o.getExpiresAt())
                .createdAt(o.getCreatedAt())
                .items(o.getItems().stream().map(i -> OrderResponse.OrderItemResponse.builder()
                        .id(i.getId())
                        .ticketTypeName(i.getTicketType().getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build()).toList())
                .build();
    }

    private TicketResponse toTicketResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .ticketCode(t.getTicketCode())
                .qrCodeBase64(t.getQrCodeData())
                .status(t.getStatus())
                .eventTitle(t.getEvent().getTitle())
                .eventVenue(t.getEvent().getVenueName())
                .eventStartsAt(t.getEvent().getStartsAt())
                .ticketTypeName(t.getTicketType().getName())
                .issuedAt(t.getIssuedAt())
                .checkedInAt(t.getCheckedInAt())
                .build();
    }
}
