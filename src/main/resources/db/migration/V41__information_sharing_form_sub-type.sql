-- This type doesn't exist in the migrations already but does exist in live environments
-- Inserting it here (and ignoring conflicts) ensures parity across all environments and tests
insert
    into case_note_type(code, description, created_at, created_by)
values ('OMIC', 'OMiC', current_date, 'OMS_OWNER') on conflict DO NOTHING;

insert
into case_note_sub_type(type_code, code, description, active, created_at, created_by,
                        sensitive, restricted_use, sync_to_nomis, dps_user_selectable)
values ('OMIC',
        'INFOSHARE_FM',
        'Information Sharing Form',
        true,
        current_date,
        'OMS_OWNER',
        false,
        true,
        true,
        true) on conflict DO NOTHING;