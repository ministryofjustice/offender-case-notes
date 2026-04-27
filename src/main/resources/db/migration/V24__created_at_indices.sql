create index if not exists idx_case_note_created_at on case_note (created_at);
create index if not exists idx_case_note_amendment_created_at on case_note_amendment (created_at);