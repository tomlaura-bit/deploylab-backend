ALTER TABLE study_group ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE assignment ADD COLUMN cancelled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE attempt ADD COLUMN assignment_id UUID REFERENCES assignment(id);
CREATE INDEX attempt_assignment_idx ON attempt(assignment_id,user_id);
CREATE INDEX notification_user_idx ON notification(user_id,created_at);
ALTER TABLE material ADD COLUMN content BYTEA;
ALTER TABLE material ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
