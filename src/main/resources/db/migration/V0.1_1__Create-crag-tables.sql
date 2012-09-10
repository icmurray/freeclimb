-- Crag schema

-- Revisions sequence generator for the whole table.  It's not important
-- that a given crag's revision history is sequential, only that it's
-- striclty increasing.increasing.
CREATE SEQUENCE crag_revision_seq START 1;

-- The simplest of crag tables.
CREATE TABLE crags (
    id serial PRIMARY KEY,
    name varchar(60) NOT NULL UNIQUE CHECK (name <> ''),
    title varchar(100) NOT NULL CHECK (title <> ''),
    revision int NOT NULL CHECK (revision > 0) DEFAULT nextval('crag_revision_seq')
);

-- Set the owner of the sequence to the table column.
ALTER SEQUENCE crag_revision_seq OWNED BY crags.revision;

-- Mirror of the crag table, with a few extra columns thrown in.
-- Note that crag_id does not reference crags.  This is because we want to
-- be able to delete rows from the "crgas" table without deleting their
-- history from the "crag_history" table.
CREATE TABLE crag_history (
    
    -- mirror the "crags" table.
    -- remove most of the constraints as they may change on the "crags"
    -- table.
    id serial PRIMARY KEY,
    name varchar(60) NOT NULL,
    title varchar(100) NOT NULL,
    revision int NOT NULL CHECK (revision > 0),

    -- onto the history data
    crag_id int,    -- note that this does *not* reference crags.id
    timestamp timestamp without time zone NOT NULL DEFAULT localtimestamp,

    UNIQUE (crag_id, revision)
);

-- Setup a trigger that records inserts and updates into "crag_history".
CREATE FUNCTION record_crag() RETURNS TRIGGER AS $BODY$
BEGIN
    INSERT INTO crag_history (name, title, revision, crag_id)
        VALUES (NEW.name, NEW.title, NEW.revision, NEW.id);
    RETURN NULL;
END;
$BODY$ LANGUAGE plpgsql;

CREATE TRIGGER on_crag_insert_or_update
    AFTER INSERT OR UPDATE ON crags
    FOR EACH ROW EXECUTE PROCEDURE record_crag();
