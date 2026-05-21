ALTER TABLE outbox_event
    ADD COLUMN join_code VARCHAR(4) NULL;

CREATE INDEX idx_outbox_join_code_status ON outbox_event (join_code, status);
