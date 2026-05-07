-- Disable DPS user selection for specific RESET - Resettlement case note sub-types
UPDATE case_note_sub_type SET dps_user_selectable = false
WHERE type_code = 'RESET' AND code IN ('ACCOM', 'ATB', 'BCST', 'CHDFAMCOM', 'DRUG_ALCOHOL', 'ED_SKL_WRK', 'FINANCE_ID', 'HEALTH');