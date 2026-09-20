-- Resolution of the incident and submission of the evaluation are separate operations.
ALTER TABLE attempt ADD COLUMN finalized BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE attempt_evaluation (
 attempt_id UUID PRIMARY KEY REFERENCES attempt(id),
 score INTEGER NOT NULL CHECK (score BETWEEN 0 AND 100),
 mistakes INTEGER NOT NULL, hints INTEGER NOT NULL, solved BOOLEAN NOT NULL,
 explanation VARCHAR(2000) NOT NULL,
 evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Preserve the evaluations created by the original automatic-finalization implementation.
INSERT INTO attempt_evaluation(attempt_id,score,mistakes,hints,solved,explanation,evaluated_at)
 SELECT a.id,a.score,a.mistakes,a.hints,TRUE,s.explanation,a.completed_at
 FROM attempt a JOIN scenario s ON s.id=a.scenario_id
 WHERE a.completed_at IS NOT NULL;
UPDATE attempt SET finalized=TRUE WHERE completed_at IS NOT NULL;
