CREATE TABLE saksdata (
    id UUID PRIMARY KEY,
    kabal_reference UUID NOT NULL,
    sakstype VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE FUNCTION update_saksdata_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER saksdata_updated_at
BEFORE UPDATE ON saksdata
FOR EACH ROW
EXECUTE FUNCTION update_saksdata_updated_at();
