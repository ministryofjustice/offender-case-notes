-- Enable DPS user selection for RESET - Resettlement, FINANCE_ID - Finance and ID case note sub-type
UPDATE case_note_sub_type SET dps_user_selectable = true
WHERE type_code = 'RESET' AND code = 'FINANCE_ID';