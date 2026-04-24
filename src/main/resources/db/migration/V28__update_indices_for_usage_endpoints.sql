create index if not exists idx_location_sub_type_occurred_at on case_note (lower(location_id), sub_type_id, occurred_at);
create index if not exists idx_author_id_sub_type_occurred_at on case_note(author_user_id, sub_type_id, occurred_at);

drop index if exists idx_author_id_sub_type;
