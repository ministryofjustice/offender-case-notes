-- Enable DPS user selection for specific RESET - Resettlement case note sub-types
-- Reverts V34__resettlement_passport_sub-types_dps_user_selectable_false following discovery of out of service usage of these sub-types by prisons
UPDATE case_note_sub_type SET dps_user_selectable = true
WHERE type_code = 'RESET' AND code IN ('ACCOM', 'ATB', 'BCST', 'CHDFAMCOM', 'DRUG_ALCOHOL', 'ED_SKL_WRK', 'FINANCE_ID', 'HEALTH');