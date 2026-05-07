create index idx_case_note_prison_number_lower on offender_case_note (lower(offender_identifier));
create index idx_case_note_location_id_lower on offender_case_note (lower(location_id));
create index idx_case_note_author_username_lower on offender_case_note (lower(author_username));
