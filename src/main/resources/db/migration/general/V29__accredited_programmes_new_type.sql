insert
into case_note_type(case_note_type_id, parent_type, sub_type, description, active, create_date_time, create_user_id,
                    sensitive, restricted_use, sync_to_nomis, dps_user_selectable)
values (nextval('case_note_type_case_note_type_id_seq'),
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