update case_note_sub_type
set code = 'MVD_BLD'
where code = 'MVC_BLD'
  and type_code = 'ACP';