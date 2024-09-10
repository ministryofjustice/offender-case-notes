with new_types(code, description) as (values ('REFD', 'Referred'),
                                             ('NTELG_FR_PGM', 'Not eligible for programme'),
                                             ('AW_ASSMT', 'Awaiting assessment'),
                                             ('ASSMT_STD', 'Assessment started'),
                                             ('REF_ON_HLD', 'Referral on hold'),
                                             ('SUT_FR_PGM', 'Suitable for programme'),
                                             ('SUTPGM_NTRDY', 'Suitable for programme but not ready'),
                                             ('NTSUT_FR_PGM', 'Not suitable for programme'),
                                             ('REF_WTHDWN', 'Referral withdrawn'),
                                             ('PGM_STD', 'Programme started'),
                                             ('PGM_COMP', 'Programme completed'),
                                             ('PGM_NT_COMP', 'Programme not completed'))
insert
into case_note_type(case_note_type_id, parent_type, sub_type, description, active, create_date_time, create_user_id,
                    sensitive, restricted_use, sync_to_nomis, dps_user_selectable)
select nextval('case_note_type_case_note_type_id_seq'),
       'ACP',
       nt.code,
       nt.description,
       true,
       current_date,
       'OMS_OWNER',
       false,
       false,
       false,
       false
from new_types nt;