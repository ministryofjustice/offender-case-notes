ALTER TABLE offender_case_note
    ADD author_user_id VARCHAR(36) NULL;

UPDATE offender_case_note
SET author_user_id = author_username;

ALTER TABLE offender_case_note
ALTER
author_user_id
SET NOT NULL;

ALTER TABLE offender_case_note_amendment
    ADD author_user_id VARCHAR(36) NULL;

UPDATE offender_case_note_amendment
SET author_user_id = author_username;

ALTER TABLE offender_case_note_amendment
ALTER
author_user_id
SET NOT NULL;
