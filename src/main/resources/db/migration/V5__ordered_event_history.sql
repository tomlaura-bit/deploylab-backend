ALTER TABLE attempt_event ADD COLUMN event_index INTEGER NOT NULL DEFAULT 0;
-- Use timestamp then UUID to give legacy events a deterministic order.
UPDATE attempt_event SET event_index=(
 SELECT COUNT(*) FROM attempt_event other
 WHERE other.attempt_id=attempt_event.attempt_id
 AND (other.created_at<attempt_event.created_at
 OR (other.created_at=attempt_event.created_at AND other.id<=attempt_event.id))
);
CREATE UNIQUE INDEX idx_attempt_event_order ON attempt_event(attempt_id,event_index);
