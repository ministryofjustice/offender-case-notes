DROP TABLE IF EXISTS OFFENDER_CASE_NOTE_AMENDMENT;

CREATE TABLE OFFENDER_CASE_NOTE_AMENDMENT
(
    OFFENDER_CASE_NOTE_AMENDMENT_ID SERIAL PRIMARY KEY,
    OFFENDER_CASE_NOTE_ID           BIGINT      NOT NULL,
    AMEND_SEQUENCE                  NUMERIC(5)  NOT NULL,
    STAFF_USERNAME                  VARCHAR(32) NOT NULL,
    NOTE_TEXT                       TEXT        NOT NULL,
    CREATE_DATE_TIME                TIMESTAMP   NOT NULL,
    CREATE_USER_ID                  VARCHAR(32) NOT NULL,
    MODIFY_DATE_TIME                TIMESTAMP,
    MODIFY_USER_ID                  VARCHAR(32),
    UNIQUE (OFFENDER_CASE_NOTE_ID, AMEND_SEQUENCE),
    FOREIGN KEY (OFFENDER_CASE_NOTE_ID) REFERENCES OFFENDER_CASE_NOTE (OFFENDER_CASE_NOTE_ID)
);

COMMENT ON TABLE OFFENDER_CASE_NOTE_AMENDMENT IS 'Records a case note amendments for an offender';




