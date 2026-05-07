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
