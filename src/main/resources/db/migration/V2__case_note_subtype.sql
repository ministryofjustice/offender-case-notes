DROP TABLE IF EXISTS CASE_NOTE_TYPE;

CREATE TABLE CASE_NOTE_TYPE
(
    CASE_NOTE_TYPE_ID SERIAL PRIMARY KEY,
    PARENT_TYPE       VARCHAR(12) NOT NULL,
    SUB_TYPE          VARCHAR(12) NOT NULL,
    DESCRIPTION       VARCHAR(80) NOT NULL,
    ACTIVE            BOOLEAN     NOT NULL DEFAULT true,
    CREATE_DATE_TIME  TIMESTAMP   NOT NULL,
    CREATE_USER_ID    VARCHAR(32) NOT NULL,
    MODIFY_DATE_TIME  TIMESTAMP,
    MODIFY_USER_ID    VARCHAR(32),
    FOREIGN KEY (PARENT_TYPE) REFERENCES CASE_NOTE_PARENT_TYPE (NOTE_TYPE)
);

COMMENT ON TABLE CASE_NOTE_TYPE IS 'Holds case note types allowed in this service';

CREATE UNIQUE INDEX CASE_NOTE_TYPE_UK ON CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE);





