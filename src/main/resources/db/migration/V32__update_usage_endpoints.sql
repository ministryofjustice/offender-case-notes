drop index if exists idx_person_identifier_sub_type;
drop index if exists idx_location_sub_type_occurred_at;

create index if not exists idx_person_identifier_sub_type_occurred_at_location on case_note (lower(person_identifier), sub_type_id, occurred_at, location_id);
create index if not exists idx_author_id_sub_type_occurred_at_location on case_note (author_user_id, sub_type_id, occurred_at, location_id);
create index if not exists idx_person_identifier_sub_type_created_at_location on case_note (lower(person_identifier), sub_type_id, created_at, location_id);
create index if not exists idx_author_id_sub_type_created_at_location on case_note (author_user_id, sub_type_id, created_at, location_id);