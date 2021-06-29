INSERT INTO CASE_NOTE_PARENT_TYPE (NOTE_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('POM', 'POM Notes', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_PARENT_TYPE (NOTE_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OLDPOM', 'OLDPOM Notes', false, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('POM', 'GEN', 'General POM Note', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('POM', 'SPECIAL', 'Special POM Note', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('POM', 'OLD', 'Inactive POM Note', false, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OLDPOM', 'OLDTWO', 'Inactive Old POM Note', false, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OLDPOM', 'OLDONE', 'Active Old POM Note', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_PARENT_TYPE (NOTE_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OMIC', 'OMIC Type', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OMIC', 'GEN', 'General OMIC Note', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OMIC', 'SPECIAL', 'Special OMIC Note', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, NON_SENSITIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OMIC', 'OMIC_OPEN', 'Open Case Note', true, true, now(), 'SYSTEM');


