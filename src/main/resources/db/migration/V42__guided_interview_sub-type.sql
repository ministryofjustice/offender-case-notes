insert
into case_note_sub_type(type_code, code, description, active, created_at, created_by,
                        sensitive, restricted_use, sync_to_nomis, dps_user_selectable)
values ('RESET',
        'GUIDINT',
        'Guided Interview',
        -- This is disabled for now so that it can be enabled as needed in environments
        false,
        current_date,
        'OMS_OWNER',
        false,
        false,
        true,
        true) on conflict DO NOTHING;