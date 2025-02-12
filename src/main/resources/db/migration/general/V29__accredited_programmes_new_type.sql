insert
into case_note_sub_type(id, type_code, code, description, active, created_at, created_by,
                    sensitive, restricted_use, sync_to_nomis, dps_user_selectable)
values (nextval('case_note_sub_type_id_seq'),
        'ACP',
        'MVC_BLD',
        'Moved to building choices',
        true,
        current_date,
        'OMS_OWNER',
        false,
        false,
        false,
        false);