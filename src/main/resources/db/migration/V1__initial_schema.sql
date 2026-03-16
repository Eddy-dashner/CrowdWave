-- ============================================================
-- Rwanda Ticketing App — Initial Schema
-- Hybrid user model: users + organizer_profiles + buyer_profiles
-- ============================================================

CREATE TABLE users (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name                VARCHAR(100) NOT NULL,
    last_name                 VARCHAR(100) NOT NULL,
    email                     VARCHAR(255) NOT NULL UNIQUE,
    phone                     VARCHAR(20)  UNIQUE,
    password_hash             VARCHAR(255) NOT NULL,
    role                      VARCHAR(20)  NOT NULL DEFAULT 'BUYER',
    is_active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    is_email_verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    email_verification_token  VARCHAR(255),
    password_reset_token      VARCHAR(255),
    password_reset_expires_at TIMESTAMP,
    created_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE organizer_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    business_name       VARCHAR(255) NOT NULL,
    bio                 TEXT,
    logo_url            VARCHAR(500),
    website_url         VARCHAR(500),
    instagram_handle    VARCHAR(100),
    twitter_handle      VARCHAR(100),
    is_verified         BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at         TIMESTAMP,
    payout_phone        VARCHAR(20),
    payout_provider     VARCHAR(30),
    payout_account_name VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE buyer_profiles (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    preferred_language          VARCHAR(10) NOT NULL DEFAULT 'en',
    preferred_currency          VARCHAR(3)  NOT NULL DEFAULT 'RWF',
    default_payment_phone       VARCHAR(20),
    default_payment_provider    VARCHAR(30),
    receive_email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    receive_sms_notifications   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizer_id UUID NOT NULL REFERENCES organizer_profiles(id),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    category     VARCHAR(50)  NOT NULL,
    venue_name   VARCHAR(255) NOT NULL,
    venue_address TEXT,
    city         VARCHAR(100) NOT NULL DEFAULT 'Kigali',
    banner_url   VARCHAR(500),
    starts_at    TIMESTAMP    NOT NULL,
    ends_at      TIMESTAMP    NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    is_published BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE ticket_types (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id       UUID          NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name           VARCHAR(100)  NOT NULL,
    description    TEXT,
    price          DECIMAL(12,2) NOT NULL,
    currency       VARCHAR(3)    NOT NULL DEFAULT 'RWF',
    total_quantity INTEGER       NOT NULL,
    sold_quantity  INTEGER       NOT NULL DEFAULT 0,
    sale_starts_at TIMESTAMP,
    sale_ends_at   TIMESTAMP,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE orders (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id          UUID          NOT NULL REFERENCES buyer_profiles(id),
    event_id          UUID          NOT NULL REFERENCES events(id),
    total_amount      DECIMAL(12,2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL DEFAULT 'RWF',
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_method    VARCHAR(30),
    payment_reference VARCHAR(255),
    expires_at        TIMESTAMP,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    ticket_type_id UUID          NOT NULL REFERENCES ticket_types(id),
    quantity       INTEGER       NOT NULL,
    unit_price     DECIMAL(12,2) NOT NULL,
    subtotal       DECIMAL(12,2) NOT NULL
);

CREATE TABLE tickets (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id  UUID        NOT NULL REFERENCES order_items(id),
    ticket_type_id UUID        NOT NULL REFERENCES ticket_types(id),
    event_id       UUID        NOT NULL REFERENCES events(id),
    buyer_id       UUID        NOT NULL REFERENCES buyer_profiles(id),
    ticket_code    VARCHAR(50) NOT NULL UNIQUE,
    qr_code_data   TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'VALID',
    checked_in_at  TIMESTAMP,
    checked_in_by  UUID REFERENCES users(id),
    issued_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE payments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id           UUID          NOT NULL REFERENCES orders(id),
    amount             DECIMAL(12,2) NOT NULL,
    currency           VARCHAR(3)    NOT NULL DEFAULT 'RWF',
    provider           VARCHAR(30)   NOT NULL,
    provider_reference VARCHAR(255),
    phone_number       VARCHAR(20),
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    provider_response  JSONB,
    initiated_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    completed_at       TIMESTAMP
);

-- Indexes
CREATE INDEX idx_organizer_profiles_user ON organizer_profiles(user_id);
CREATE INDEX idx_buyer_profiles_user     ON buyer_profiles(user_id);
CREATE INDEX idx_refresh_tokens_user     ON refresh_tokens(user_id);
CREATE INDEX idx_events_organizer        ON events(organizer_id);
CREATE INDEX idx_events_published_date   ON events(is_published, starts_at);
CREATE INDEX idx_ticket_types_event      ON ticket_types(event_id);
CREATE INDEX idx_orders_buyer            ON orders(buyer_id);
CREATE INDEX idx_orders_status_expires   ON orders(status, expires_at);
CREATE INDEX idx_tickets_buyer           ON tickets(buyer_id);
CREATE INDEX idx_tickets_event           ON tickets(event_id);
CREATE INDEX idx_tickets_code            ON tickets(ticket_code);
CREATE INDEX idx_payments_order          ON payments(order_id);
CREATE INDEX idx_payments_provider_ref   ON payments(provider_reference);
