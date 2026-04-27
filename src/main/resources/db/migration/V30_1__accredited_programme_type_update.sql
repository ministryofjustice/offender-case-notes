update case_note_sub_type
set code = 'MVD_BLDG'
where code = 'MVD_BLD'
  and type_code = 'ACP';