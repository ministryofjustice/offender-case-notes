DROP TABLE IF EXISTS OFFENDER_CASE_NOTE;

CREATE TABLE OFFENDER_CASE_NOTE
(
    OFFENDER_CASE_NOTE_ID SERIAL PRIMARY KEY,
    OFFENDER_IDENTIFIER   VARCHAR(12) NOT NULL,
    LOCATION_ID           VARCHAR(12) NOT NULL,
    STAFF_ID              NUMERIC     NOT NULL,
    CASE_NOTE_TYPE        VARCHAR(12) NOT NULL,
    CASE_NOTE_SUB_TYPE    VARCHAR(12) NOT NULL,
    NOTE_TEXT             TEXT,
    CREATE_DATETIME       TIMESTAMP   NOT NULL,
    CREATE_USER_ID        VARCHAR(32) NOT NULL,
    MODIFY_DATETIME       TIMESTAMP,
    MODIFY_USER_ID        VARCHAR(32)
);

COMMENT ON TABLE OFFENDER_CASE_NOTE IS 'Records a case note for an offender';


