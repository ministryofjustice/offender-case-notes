alter table CASE_NOTE_TYPE
    add SENSITIVE BOOLEAN default TRUE;

UPDATE CASE_NOTE_TYPE set SENSITIVE = TRUE;

alter table CASE_NOTE_TYPE
    ALTER column SENSITIVE SET NOT NULL;
