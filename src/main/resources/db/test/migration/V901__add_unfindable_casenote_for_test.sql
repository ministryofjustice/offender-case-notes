insert into offender_case_note (offender_case_note_id, offender_identifier, location_id, author_username, author_name,
                                case_note_type_id, occurrence_date_time, note_text, create_date_time, create_user_id,
                                author_user_id)
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