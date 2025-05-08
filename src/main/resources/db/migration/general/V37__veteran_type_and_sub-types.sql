insert
into case_note_type(code, description, created_at, created_by)
values ('VETERAN',
        'Veteran',
        current_date,
        'OMS_OWNER');

with new_types(code, description) as (values ('VET_VICSO', 'ViCSO'),
                                             ('VET_OMU', 'Offender Management Unit (OMU)'),
                                             ('VET_SC', 'Safer Custody'),
                                             ('VET_HC', 'Healthcare'),
                                             ('VET_EDU_EMP', 'Education/Employment'),
                                             ('VET_OTHER', 'Other'))
insert
into case_note_sub_type(type_code, code, description, active, created_at, created_by,
                    sensitive, restricted_use, sync_to_nomis, dps_user_selectable)
select 'VETERAN',
       nt.code,
       nt.description,
       true,
       current_date,
       'OMS_OWNER',
       false,
       false,
       true,
       true
from new_types nt;