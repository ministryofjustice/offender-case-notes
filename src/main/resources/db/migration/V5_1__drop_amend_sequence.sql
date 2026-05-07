DO $$
DECLARE
  constraint_name varchar;
BEGIN
  SELECT conname  into constraint_name
		FROM pg_constraint
		WHERE conrelid = 'offender_case_note_amendment'::regclass AND contype = 'u';

  execute 'alter table offender_case_note_amendment drop constraint ' ||  constraint_name;

  ALTER TABLE offender_case_note_amendment DROP COLUMN AMEND_SEQUENCE;
END $$;


