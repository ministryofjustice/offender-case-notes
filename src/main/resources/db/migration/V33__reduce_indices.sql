create index if not exists idx_person_identifier_sub_type_id_created_at_location_id on case_note(person_identifier, sub_type_id, created_at, location_id);
create index if not exists idx_person_identifier_sub_type_id_occurred_at_location_id on case_note(person_identifier, sub_type_id, occurred_at, location_id);

drop index if exists idx_case_note_author_username_lower;
drop index if exists idx_case_note_location_id_lower;
drop index if exists idx_case_note_person_identifier_lower;

drop index if exists idx_person_identifier_sub_type_created_at_location;
drop index if exists idx_person_identifier_sub_type_occurred_at_location;