create index if not exists idx_author_username_location_id_sub_type_id on case_note(author_username, location_id, sub_type_id);
drop index if exists idx_case_note_author_username;