-- Crag schema

-- Revision sequence generator for crag and climb models.
-- Climbs share a revision_seq with Crags in order that we can tell which
-- Climbs have been updated since a given Crag revision.
CREATE SEQUENCE revision_seq START 1;

-- The simplest of crag tables.
CREATE TABLE crags (
    id serial PRIMARY KEY,
    name varchar(60) NOT NULL UNIQUE CHECK (name <> ''),
    title varchar(100) NOT NULL CHECK (title <> ''),
    revision int NOT NULL CHECK (revision > 0) DEFAULT nextval('revision_seq')
);

-- Mirror of the crag table, with a few extra columns thrown in.
-- Note that crag_id does not reference crags.  This is because we want to
-- be able to delete rows from the "crgas" table without deleting their
-- history from the "crag_history" table.
CREATE TABLE crag_history (
    
    -- mirror the "crags" table.
    -- remove most of the constraints as they may change on the "crags"
    -- table.
    revision int PRIMARY KEY,
    name varchar(60) NOT NULL,
    title varchar(100) NOT NULL,

    -- onto the history data
    crag_id int,    -- note that this does *not* reference crags.id
    timestamp timestamp without time zone NOT NULL DEFAULT localtimestamp
);

-- Setup a trigger that records inserts and updates into "crag_history".
-- The function is declared only to record new crags (INSERTS), or UPDATES
-- which change at least one column (other than the revision column).  The
-- reason for this is that inserting or updating a climb will bump the
-- revision of the referenced crag.  And we don't want to record that change
-- in the crag's history.  This of course means that we don't record a
-- crag update that doesn't actually change anything, but I think that's ok.
CREATE FUNCTION record_crag() RETURNS TRIGGER AS $BODY$
BEGIN

    IF (TG_OP = 'INSERT') THEN
        INSERT INTO crag_history (revision, name, title, crag_id)
            VALUES (NEW.revision, NEW.name, NEW.title, NEW.id);

    ELSIF (TG_OP = 'UPDATE') THEN
       
        IF (OLD.title <> NEW.title OR
            OLD.name <> NEW.name) THEN
        
            INSERT INTO crag_history (revision, name, title, crag_id)
                VALUES (NEW.revision, NEW.name, NEW.title, NEW.id);

        ELSE
            -- Note that we're not updating the timestamp.
            -- Although this breaks the relationship between timestamp
            -- and revision *in general*:
            --
            --   rev1 < rev2 => t1 <= t2
            --
            -- However, it still holds for any given Crag's history. So it's
            -- ok to do this.  Also, if the timestamp *were* to be updated,
            -- then the timestamps of the last actual update to the crag
            -- (rather than the update to a climb it owns) would be lost.
            UPDATE crag_history SET
                revision = NEW.revision
            WHERE crag_id = NEW.id
              AND revision = OLD.revision;

        END IF;

    END IF;

    RETURN NULL;
END;
$BODY$ LANGUAGE plpgsql;

CREATE TRIGGER on_crag_insert_or_update
    AFTER INSERT OR UPDATE ON crags
    FOR EACH ROW EXECUTE PROCEDURE record_crag();
