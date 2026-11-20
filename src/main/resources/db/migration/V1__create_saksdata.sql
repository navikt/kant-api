CREATE TABLE saksdata (
    id UUID PRIMARY KEY,
    kabal_reference UUID NOT NULL,
    sakstype_id TEXT NOT NULL,
    ytelse_id TEXT,
    utfall_id TEXT,
    dato_saksdata_avsluttet_av_saksbehandler TIMESTAMP,
    dato_mottatt_klageinstans DATE,
    sak_i_trygderetten_opprettet DATE,
    tilknyttet_enhet TEXT,
    tilbakekreving BOOLEAN,
    kvalitetsvurdering JSONB,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE saksdata
    ADD CONSTRAINT saksdata_kabal_reference_unique UNIQUE (kabal_reference);

CREATE TABLE registreringshjemmel (
    saksdata_id UUID NOT NULL REFERENCES saksdata(id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    PRIMARY KEY (saksdata_id, id),
    CONSTRAINT fk_registreringshjemmel_saksdata
        FOREIGN KEY (saksdata_id)
            REFERENCES saksdata(id)
);

CREATE FUNCTION update_saksdata_modified()
RETURNS TRIGGER AS $$
BEGIN
    NEW.modified = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER saksdata_modified
BEFORE UPDATE ON saksdata
FOR EACH ROW
EXECUTE FUNCTION update_saksdata_modified();
