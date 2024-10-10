insert into case_note (id, person_identifier, location_id, author_username, author_name, sub_type_id, occurred_at,
                       note_text, created_at, created_by, author_user_id, system)
select gen_random_uuid(),
       'S1234TN',
       'MDI',
       'SYS',
       'SYS',
       st.id,
       current_date,
       'A case note that should not be visible',
       current_date,
       'SYS',
       'SYS',
       'DPS'
from case_note_sub_type st
where type_code = 'CAB'
  and code = 'EDUCATION';