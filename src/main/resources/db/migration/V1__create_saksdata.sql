CREATE TABLE saksdata (
    id UUID PRIMARY KEY,
    kabal_reference UUID NOT NULL,
    sakstype_id TEXT NOT NULL,
    ytelse_id TEXT,
    utfall_id TEXT,
    dato_saksdata_avsluttet_av_saksbehandler TIMESTAMP,
    created TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SCHEMA kaka;

CREATE TABLE kaka.registreringshjemmel (
    saksdata_id UUID NOT NULL REFERENCES saksdata(id) ON DELETE CASCADE,
    id TEXT NOT NULL,
    PRIMARY KEY (saksdata_id, id)
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
