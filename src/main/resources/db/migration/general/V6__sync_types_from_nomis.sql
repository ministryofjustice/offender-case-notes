update case_note_type
set active = false
from case_note_type nt
         join case_note_parent_type pt on pt.note_type = nt.parent_type
where nt.active = true
  and pt.active = false;

alter table case_note_parent_type
    drop column active;

alter table case_note_type
    add column sync_to_nomis       boolean not null default false,
    add column dps_user_selectable boolean not null default true;