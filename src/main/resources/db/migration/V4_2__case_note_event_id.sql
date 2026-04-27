CREATE SEQUENCE offender_case_note_event_id_seq INCREMENT BY -1 START WITH -1;

ALTER TABLE offender_case_note
    ADD event_id BIGINT NOT NULL DEFAULT NEXTVAL('offender_case_note_event_id_seq');
