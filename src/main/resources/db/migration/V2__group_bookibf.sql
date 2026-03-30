ALTER TABLE ticket_types
    ADD COLUMN is_group_ticket BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN min_group_size INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN max_group_size INTEGER NOT NULL DEFAULT 1;

CREATE TABLE ticket_attendees (
                                  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  ticket_id       UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
                                  first_name      VARCHAR(100) NOT NULL,
                                  last_name       VARCHAR(100) NOT NULL,
                                  email           VARCHAR(255) NOT NULL,
                                  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ticket_attendees_ticket ON ticket_attendees(ticket_id);
CREATE INDEX idx_ticket_attendees_email  ON ticket_attendees(email);