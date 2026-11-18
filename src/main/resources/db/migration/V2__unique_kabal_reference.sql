ALTER TABLE saksdata
    ADD CONSTRAINT saksdata_kabal_reference_unique UNIQUE (kabal_reference),
    ADD COLUMN dato_mottatt_klageinstans DATE,
    ADD COLUMN sak_i_trygderetten_opprettet DATE,
    ADD COLUMN tilknyttet_enhet TEXT,
    ADD COLUMN tilbakekreving BOOLEAN,
    ADD COLUMN kvalitetsvurdering JSONB;
