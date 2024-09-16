insert into case_note (id, person_identifier, location_id, author_username, author_name, type_id, occurred_at,
                       note_text, created_at, created_by, author_user_id)
select gen_random_uuid(),
       'S1234TN',
       'MDI',
       'SYS',
       'SYS',
       case_note_type_id,
       current_date,
       'A case note that should not be visible',
       current_date,
       'SYS',
       'SYS'
from case_note_type
where parent_type = 'CAB'
  and sub_type = 'EDUCATION';