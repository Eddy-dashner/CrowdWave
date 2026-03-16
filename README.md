# Rwanda Ticketing App — Backend

Spring Boot 3.2 · PostgreSQL · JWT · MTN MoMo / Airtel Money

---

## Project Structure

```
src/main/java/rw/ticketing/
├── config/           # Security, Async, WebClient, OrderExpiryJob
├── controller/       # REST controllers (Auth, Event, Order, Ticket, Payment)
├── dto/
│   ├── request/      # Incoming request bodies
│   └── response/     # Outgoing response bodies
├── entity/           # JPA entities
├── enums/            # Role, EventStatus, OrderStatus, etc.
├── exception/        # AppException + GlobalExceptionHandler
├── repository/       # Spring Data JPA repositories
├── security/         # JwtAuthFilter
├── service/impl/     # Business logic (Auth, Event, Order, Payment, Email)
└── util/             # JwtUtil, QrCodeUtil, TicketCodeUtil
```

---

## Prerequisites

- Java 17+
- PostgreSQL 14+
- Maven 3.8+

---

## Quick Start

### 1. Create the database

```sql
CREATE DATABASE ticketing_db;
```

### 2. Set environment variables

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=yourpassword
export JWT_SECRET=your-super-secret-key-at-least-32-chars-long!!
export MAIL_USERNAME=your@gmail.com
export MAIL_PASSWORD=your-app-password
export MOMO_COLLECTION_KEY=your-mtn-momo-subscription-key
export APP_BASE_URL=http://localhost:8080
export FRONTEND_URL=http://localhost:3000
```

### 3. Run

```bash
mvn spring-boot:run
```

Flyway runs the migration automatically on startup and creates all tables.

---

## API Reference

### Auth  `/api/v1/auth`

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/register` | RegisterRequest | Register as BUYER or ORGANIZER |
| POST | `/login` | LoginRequest | Get access + refresh tokens |
| POST | `/refresh` | RefreshTokenRequest | Rotate tokens |
| GET | `/verify-email?token=` | — | Verify email from link |
| POST | `/forgot-password` | ForgotPasswordRequest | Send reset email |
| POST | `/reset-password` | ResetPasswordRequest | Set new password |

**RegisterRequest example:**
```json
{
  "firstName": "Amani",
  "lastName": "Mugabo",
  "email": "amani@example.com",
  "phone": "+250781234567",
  "password": "secure123",
  "role": "ORGANIZER",
  "businessName": "Kigali Events Ltd"
}
```

**AuthResponse example:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "uuid-string",
  "tokenType": "Bearer",
  "userId": "...",
  "email": "amani@example.com",
  "fullName": "Amani Mugabo",
  "role": "ORGANIZER",
  "hasOrganizerProfile": true,
  "hasBuyerProfile": false
}
```

---

### Events  `/api/v1/events`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | Public | List upcoming published events (paginated) |
| GET | `/?q=kigali` | Public | Search events |
| GET | `/{id}` | Public | Get single event |
| POST | `/` | ORGANIZER | Create event with ticket types |
| GET | `/my` | ORGANIZER | List my events |
| PATCH | `/{id}/publish` | ORGANIZER | Publish a draft event |
| PATCH | `/{id}/cancel` | ORGANIZER | Cancel an event |

**CreateEventRequest example:**
```json
{
  "title": "Kigali Jazz Night 2025",
  "description": "An evening of smooth jazz at Kigali Convention Centre.",
  "category": "CONCERT",
  "venueName": "Kigali Convention Centre",
  "venueAddress": "KG 2 Roundabout, Kigali",
  "city": "Kigali",
  "startsAt": "2025-08-15T19:00:00",
  "endsAt": "2025-08-15T23:00:00",
  "ticketTypes": [
    {
      "name": "Regular",
      "price": 15000,
      "totalQuantity": 500
    },
    {
      "name": "VIP",
      "description": "Front row seats + complimentary drink",
      "price": 50000,
      "totalQuantity": 50
    }
  ]
}
```

---

### Orders  `/api/v1/orders`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/` | BUYER | Place an order (15-min payment window) |
| GET | `/` | BUYER | My order history |

**PlaceOrderRequest example:**
```json
{
  "eventId": "event-uuid",
  "items": [
    { "ticketTypeId": "type-uuid", "quantity": 2 }
  ]
}
```

---

### Payments  `/api/v1/payments`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/initiate` | BUYER | Trigger MoMo/Airtel USSD push |
| GET | `/status/{orderId}` | BUYER | Poll payment status |
| PUT | `/callback/momo/{ref}` | Public | MTN MoMo webhook |
| POST | `/callback/airtel/{ref}` | Public | Airtel Money webhook |

**InitiatePaymentRequest example:**
```json
{
  "orderId": "order-uuid",
  "provider": "MTN_MOMO",
  "phoneNumber": "+250781234567"
}
```

**Payment flow:**
1. Buyer places an order → gets `orderId`
2. Buyer calls `/payments/initiate` → USSD prompt sent to phone
3. Buyer approves on phone → MTN/Airtel calls our `/callback/momo/{ref}`
4. Callback confirms payment → tickets are issued → confirmation email sent
5. Frontend polls `/payments/status/{orderId}` to show success screen

---

### Tickets  `/api/v1/tickets`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | BUYER | My tickets (with QR codes) |
| GET | `/{id}` | BUYER | Single ticket detail |
| POST | `/check-in/{code}` | ORGANIZER | Scan & check in at gate |

**CheckInResponse example:**
```json
{
  "success": true,
  "ticketCode": "RWT-AB3X-KP7M-Q2NV",
  "attendeeName": "Bella Uwimana",
  "eventTitle": "Kigali Jazz Night 2025",
  "ticketTypeName": "VIP",
  "previousStatus": "VALID",
  "message": "Check-in successful"
}
```

---

## User Model

```
users                    ← identity (email, password, role)
  │
  ├─ organizer_profiles  ← business info, payout details (role = ORGANIZER)
  │      └─ events
  │              └─ ticket_types
  │
  └─ buyer_profiles      ← preferences, default payment (role = BUYER)
         └─ orders
                └─ order_items
                └─ tickets
```

A user can have both profiles if they organise events AND buy tickets.

---

## Ticket Code Format

```
RWT-AB3X-KP7M-Q2NV
```

- `RWT` prefix identifies Rwanda Tickets
- 3 segments of 4 alphanumeric chars (no O, 0, I, 1 to avoid confusion)
- ~1.8 billion unique combinations

---

## MTN MoMo Setup

1. Register at https://momodeveloper.mtn.com
2. Subscribe to the **Collection** product
3. Create a sandbox API user + API key via the sandbox provisioning API
4. Set `MOMO_COLLECTION_KEY` env var
5. For production: change `momo.api.environment=sandbox` → `mtncongo` / `mtnrwanda`

---

## Next Steps

- [ ] Add `ADMIN` dashboard endpoints (verify organizers, view all orders)
- [ ] Add organizer payout/settlement flow
- [ ] Add event capacity alerts
- [ ] Add Swagger/OpenAPI docs (`springdoc-openapi`)
- [ ] Write integration tests
- [ ] Dockerize with `docker-compose.yml`
