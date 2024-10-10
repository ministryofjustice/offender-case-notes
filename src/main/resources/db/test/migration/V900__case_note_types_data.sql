insert into case_note_type (code, description, created_at, created_by)
values ('OLDPOM', 'OLDPOM Notes', now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, created_at, created_by)
values ('POM', 'GEN', 'General POM Note', true, now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, created_at, created_by)
values ('POM', 'SPECIAL', 'Special POM Note', true, now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, created_at, created_by)
values ('POM', 'OLD', 'Inactive POM Note', false, now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, created_at, created_by)
values ('OLDPOM', 'OLDTWO', 'Inactive Old POM Note', false, now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, created_at, created_by)
values ('OLDPOM', 'OLDONE', 'Active Old POM Note', false, now(), 'SYSTEM');

insert into case_note_type (code, description, created_at, created_by)
values ('OMIC', 'OMIC Type', now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, created_at, created_by)
values ('OMIC', 'GEN', 'General OMIC Note', true, now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, created_at, created_by)
values ('OMIC', 'SPECIAL', 'Special OMIC Note', true, now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, sensitive, created_at, created_by)
values ('OMIC', 'OMIC_OPEN', 'Open Case Note', true, false, now(), 'SYSTEM');

insert into case_note_type (code, description, created_at, created_by)
values ('READ_TEST', 'Parent Type for testing', now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, sensitive, restricted_use, created_at, created_by)
values ('READ_TEST', 'ACT_SEN', 'Active & Sensitive', true, true, false, now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, sensitive, restricted_use, created_at, created_by)
values ('READ_TEST', 'INA_SEN', 'Inactive & Sensitive', false, true, false, now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, sensitive, restricted_use, created_at, created_by)
values ('READ_TEST', 'ACT_RES', 'Active & Restricted', true, false, true, now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, sensitive, restricted_use, created_at, created_by)
values ('READ_TEST', 'INA_RES', 'Inactive & Restricted', false, false, true, now(), 'SYSTEM');

insert into case_note_type (code, description, created_at, created_by)
values ('NOT_DPS', 'Non DPS Parent', now(), 'SYSTEM');

insert into case_note_sub_type (type_code, code, description, active, sensitive, restricted_use, dps_user_selectable,
                                created_at, created_by)
values ('NOT_DPS', 'NOT_SEL', 'Not Dps User Selectable', true, false, false, false, now(), 'SYSTEM');

--Insert statement case_note

insert into case_note (id, person_identifier, location_id, author_username, author_name, sub_type_id, occurred_at,
                       note_text, created_at, created_by, legacy_id, author_user_id, system)
values ('b5aa1318-e874-4614-a80d-a514e84d1a52', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith',
        (select id from case_note_sub_type where type_code = 'POM' and code = 'GEN'),
        '2019-08-29 17:08:46.188956', 'text', '2019-08-29 17:08:46.655233', 'ADAM_GEN',
        nextval('case_note_legacy_id_seq'), 'ADAM_GEN', 'DPS');

insert into case_note (id, person_identifier, location_id, author_username, author_name, sub_type_id, occurred_at,
                       note_text, created_at, created_by, legacy_id, author_user_id, system)
values ('b5aa1318-e874-4614-a80d-a514e84d1a53', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith',
        (select id from case_note_sub_type where type_code = 'POM' and code = 'GEN'),
        '2022-07-29 11:08:46.655233', 'text1', '2022-07-29 11:08:46.655233', 'ADAM_GEN',
        nextval('case_note_legacy_id_seq'), 'ADAM_GEN', 'DPS');


insert into case_note (id, person_identifier, location_id, author_username, author_name, sub_type_id, occurred_at,
                       note_text, created_at, created_by, legacy_id, author_user_id, system)
values ('b5aa1318-e874-4614-a80d-a514e84d1a54', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith',
        (select id from case_note_sub_type where type_code = 'POM' and code = 'GEN'),
        '2023-07-29 17:08:46.655233', 'text33', '2023-07-29 17:08:46.655233', 'ADAM_GEN',
        nextval('case_note_legacy_id_seq'), 'ADAM_GEN', 'DPS');

insert into case_note (id, person_identifier, location_id, author_username, author_name, sub_type_id, occurred_at,
                       note_text, created_at, created_by, legacy_id, author_user_id, system)
values ('b5aa1318-e874-4614-a80d-a514e84d1a55', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith',
        (select id from case_note_sub_type where type_code = 'POM' and code = 'GEN'),
        '2023-11-29 17:08:46.655233', 'text4', '2023-11-29 17:08:46.655233', 'ADAM_GEN',
        nextval('case_note_legacy_id_seq'), 'ADAM_GEN', 'DPS');

insert into case_note (id, person_identifier, location_id, author_username, author_name, sub_type_id, occurred_at,
                       note_text, created_at, created_by, legacy_id, author_user_id, system)
values ('b5aa1318-e874-4614-a80d-a514e84d1a56', 'L6962XX', 'WNI', 'ADAM_GEN', 'John Smith',
        (select id from case_note_sub_type where type_code = 'POM' and code = 'GEN'),
        '2023-12-30 17:08:46.655233', 'text6', '2023-12-30 17:08:46.655233', 'ADAM_GEN',
        nextval('case_note_legacy_id_seq'), 'ADAM_GEN', 'DPS');

--Insert statement case_note_amendment

insert into case_note_amendment (id, case_note_id, author_username, author_name, note_text, created_at, created_by,
                                 author_user_id, system)
values (gen_random_uuid(), 'b5aa1318-e874-4614-a80d-a514e84d1a54', 'ADAM_GEN', 'John Smith', 'Note amendment1',
        '2023-12-29 17:08:46.655233', 'ADAM_GEN',
        'ADAM_GEN', 'DPS');


insert into case_note_amendment (id, case_note_id, author_username, author_name, note_text, created_at, created_by,
                                 author_user_id, system)
values (gen_random_uuid(), 'b5aa1318-e874-4614-a80d-a514e84d1a54', 'ADAM_GEN', 'John Smith', 'Note amendment2',
        '2024-01-29 17:08:46.655233', 'ADAM_GEN',
        'ADAM_GEN', 'DPS');

insert into case_note_amendment (id, case_note_id, author_username, author_name, note_text, created_at, created_by,
                                 author_user_id, system)
values (gen_random_uuid(), 'b5aa1318-e874-4614-a80d-a514e84d1a56', 'ADAM_GEN', 'John Smith', 'New amendment1',
        '2023-12-31 17:08:46.655233', 'ADAM_GEN',
        'ADAM_GEN', 'DPS');


