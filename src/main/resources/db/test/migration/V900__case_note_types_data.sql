INSERT INTO CASE_NOTE_PARENT_TYPE (NOTE_TYPE, DESCRIPTION, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OLDPOM', 'OLDPOM Notes', now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('POM', 'GEN', 'General POM Note', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('POM', 'SPECIAL', 'Special POM Note', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('POM', 'OLD', 'Inactive POM Note', false, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OLDPOM', 'OLDTWO', 'Inactive Old POM Note', false, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OLDPOM', 'OLDONE', 'Active Old POM Note', false, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_PARENT_TYPE (NOTE_TYPE, DESCRIPTION, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OMIC', 'OMIC Type', now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OMIC', 'GEN', 'General OMIC Note', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OMIC', 'SPECIAL', 'Special OMIC Note', true, now(), 'SYSTEM');

INSERT INTO CASE_NOTE_TYPE (PARENT_TYPE, SUB_TYPE, DESCRIPTION, ACTIVE, SENSITIVE, CREATE_DATE_TIME, CREATE_USER_ID)
VALUES ('OMIC', 'OMIC_OPEN', 'Open Case Note', true, false, now(), 'SYSTEM');

insert into case_note_parent_type (note_type, description, create_date_time, create_user_id)
values ('READ_TEST', 'Parent Type for testing', now(), 'SYSTEM');

insert into case_note_type (parent_type, sub_type, description, active, sensitive, restricted_use, create_date_time, create_user_id)
values ('READ_TEST', 'ACT_SEN', 'Active & Sensitive', true, true, false, now(), 'SYSTEM');

insert into case_note_type (parent_type, sub_type, description, active, sensitive, restricted_use, create_date_time, create_user_id)
values ('READ_TEST', 'INA_SEN', 'Inactive & Sensitive', false, true, false, now(), 'SYSTEM');

insert into case_note_type (parent_type, sub_type, description, active, sensitive, restricted_use, create_date_time, create_user_id)
values ('READ_TEST', 'ACT_RES', 'Active & Restricted', true, false, true, now(), 'SYSTEM');

insert into case_note_type (parent_type, sub_type, description, active, sensitive, restricted_use, create_date_time, create_user_id)
values ('READ_TEST', 'INA_RES', 'Inactive & Restricted', false, false, true, now(), 'SYSTEM');

insert into case_note_parent_type (note_type, description, create_date_time, create_user_id)
values ('NOT_DPS', 'Non DPS Parent', now(), 'SYSTEM');

insert into case_note_type (parent_type, sub_type, description, active, sensitive, restricted_use, dps_user_selectable, create_date_time, create_user_id)
values ('NOT_DPS', 'NOT_SEL', 'Not Dps User Selectable', true, false, false, false, now(), 'SYSTEM');

--Insert statement OFFENDER_CASE_NOTE

INSERT INTO OFFENDER_CASE_NOTE (OFFENDER_CASE_NOTE_ID, OFFENDER_IDENTIFIER, LOCATION_ID, AUTHOR_USERNAME, AUTHOR_NAME, CASE_NOTE_TYPE_ID,OCCURRENCE_DATE_TIME,
                                NOTE_TEXT, CREATE_DATE_TIME, CREATE_USER_ID, MODIFY_DATE_TIME, MODIFY_USER_ID, EVENT_ID, AUTHOR_USER_ID, SOFT_DELETED)
VALUES ('b5aa1318-e874-4614-a80d-a514e84d1a52', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith', (select case_note_type_id from case_note_type where parent_type = 'POM' and sub_type = 'GEN'),
        '2019-08-29 17:08:46.188956', 'text','2019-08-29 17:08:46.655233','ADAM_GEN','2019-08-29 17:08:46.655233','ADAM_GEN', -1, 'ADAM_GEN', 'FALSE');

INSERT INTO OFFENDER_CASE_NOTE (OFFENDER_CASE_NOTE_ID, OFFENDER_IDENTIFIER, LOCATION_ID, AUTHOR_USERNAME, AUTHOR_NAME, CASE_NOTE_TYPE_ID,OCCURRENCE_DATE_TIME,
                                NOTE_TEXT, CREATE_DATE_TIME, CREATE_USER_ID, MODIFY_DATE_TIME, MODIFY_USER_ID, EVENT_ID, AUTHOR_USER_ID, SOFT_DELETED)
VALUES ('b5aa1318-e874-4614-a80d-a514e84d1a53', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith', (select case_note_type_id from case_note_type where parent_type = 'POM' and sub_type = 'GEN'),
        '2022-07-29 11:08:46.655233', 'text1','2022-07-29 11:08:46.655233','ADAM_GEN','2022-07-29 11:08:46.655233','ADAM_GEN', -2, 'ADAM_GEN', 'FALSE');


INSERT INTO OFFENDER_CASE_NOTE (OFFENDER_CASE_NOTE_ID, OFFENDER_IDENTIFIER, LOCATION_ID, AUTHOR_USERNAME, AUTHOR_NAME, CASE_NOTE_TYPE_ID,OCCURRENCE_DATE_TIME,
                                NOTE_TEXT, CREATE_DATE_TIME, CREATE_USER_ID, MODIFY_DATE_TIME, MODIFY_USER_ID, EVENT_ID, AUTHOR_USER_ID, SOFT_DELETED)
VALUES ('b5aa1318-e874-4614-a80d-a514e84d1a54', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith', (select case_note_type_id from case_note_type where parent_type = 'POM' and sub_type = 'GEN'),
        '2023-07-29 17:08:46.655233', 'text33','2023-07-29 17:08:46.655233','ADAM_GEN','2023-07-29 17:08:46.655233','ADAM_GEN', -3, 'ADAM_GEN', 'FALSE');

INSERT INTO OFFENDER_CASE_NOTE (OFFENDER_CASE_NOTE_ID, OFFENDER_IDENTIFIER, LOCATION_ID, AUTHOR_USERNAME, AUTHOR_NAME, CASE_NOTE_TYPE_ID,OCCURRENCE_DATE_TIME,
                                NOTE_TEXT, CREATE_DATE_TIME, CREATE_USER_ID, MODIFY_DATE_TIME, MODIFY_USER_ID, EVENT_ID, AUTHOR_USER_ID, SOFT_DELETED)
VALUES ('b5aa1318-e874-4614-a80d-a514e84d1a55', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith', (select case_note_type_id from case_note_type where parent_type = 'POM' and sub_type = 'GEN'),
        '2023-11-29 17:08:46.655233', 'text4','2023-11-29 17:08:46.655233','ADAM_GEN','2023-11-29 17:08:46.655233','ADAM_GEN', -4, 'ADAM_GEN', 'FALSE');

INSERT INTO OFFENDER_CASE_NOTE (OFFENDER_CASE_NOTE_ID, OFFENDER_IDENTIFIER, LOCATION_ID, AUTHOR_USERNAME, AUTHOR_NAME, CASE_NOTE_TYPE_ID,OCCURRENCE_DATE_TIME,
                                NOTE_TEXT, CREATE_DATE_TIME, CREATE_USER_ID, MODIFY_DATE_TIME, MODIFY_USER_ID, EVENT_ID, AUTHOR_USER_ID, SOFT_DELETED)
VALUES ('b5aa1318-e874-4614-a80d-a514e84d1a56', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith', (select case_note_type_id from case_note_type where parent_type = 'POM' and sub_type = 'GEN'),
        '2023-12-30 17:08:46.655233', 'text6','2023-12-30 17:08:46.655233','ADAM_GEN','2023-12-30 17:08:46.655233','ADAM_GEN', -5, 'ADAM_GEN', 'FALSE');

--Insert statement OFFENDER_CASE_NOTE_AMENDMENT

INSERT INTO OFFENDER_CASE_NOTE_AMENDMENT (OFFENDER_CASE_NOTE_AMENDMENT_ID, OFFENDER_CASE_NOTE_ID, AUTHOR_USERNAME,
                                          AUTHOR_NAME, NOTE_TEXT, CREATE_DATE_TIME, CREATE_USER_ID, MODIFY_DATE_TIME,
                                          MODIFY_USER_ID, AUTHOR_USER_ID, SOFT_DELETED)
VALUES (-21, 'b5aa1318-e874-4614-a80d-a514e84d1a54','ADAM_GEN','John Smith','Note amendment1','2023-12-29 17:08:46.655233','ADAM_GEN','2023-07-29 17:08:46.655233',
       'ADAM_GEN','ADAM_GEN', 'FALSE');


INSERT INTO OFFENDER_CASE_NOTE_AMENDMENT (OFFENDER_CASE_NOTE_AMENDMENT_ID, OFFENDER_CASE_NOTE_ID, AUTHOR_USERNAME,
                                          AUTHOR_NAME, NOTE_TEXT, CREATE_DATE_TIME, CREATE_USER_ID, MODIFY_DATE_TIME,
                                          MODIFY_USER_ID, AUTHOR_USER_ID, SOFT_DELETED)
VALUES (-22, 'b5aa1318-e874-4614-a80d-a514e84d1a54','ADAM_GEN','John Smith','Note amendment2','2024-01-29 17:08:46.655233','ADAM_GEN','2024-01-29 17:08:46.655233',
        'ADAM_GEN','ADAM_GEN', 'FALSE');

INSERT INTO OFFENDER_CASE_NOTE_AMENDMENT (OFFENDER_CASE_NOTE_AMENDMENT_ID, OFFENDER_CASE_NOTE_ID, AUTHOR_USERNAME,
                                          AUTHOR_NAME, NOTE_TEXT, CREATE_DATE_TIME, CREATE_USER_ID, MODIFY_DATE_TIME,
                                          MODIFY_USER_ID, AUTHOR_USER_ID, SOFT_DELETED)
VALUES (-23, 'b5aa1318-e874-4614-a80d-a514e84d1a56','ADAM_GEN','John Smith','New amendment1','2023-12-31 17:08:46.655233','ADAM_GEN','2023-12-31 17:08:46.655233',
        'ADAM_GEN','ADAM_GEN', 'FALSE');


