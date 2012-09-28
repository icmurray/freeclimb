-- Climb schema

-- The simplest of climb tables.
CREATE TABLE climbs (
    id serial PRIMARY KEY,
    name varchar(60) NOT NULL,
    title varchar(100) NOT NULL CHECK (title <> ''),
    description text NOT NULL,
    crag_id int NOT NULL REFERENCES crags(id),
    revision int NOT NULL CHECK (revision > 0) DEFAULT nextval('revision_seq'),

    grade_id int NOT NULL REFERENCES grades(id),

    UNIQUE (name, crag_id)
);

-- Mirror of the climb table, with a few extra columns thrown in.
-- Note that climb does not reference climbs.  This is because we want to
-- be able to delete rows from the "climbs" table without deleting their
-- history from the "climb_history" table.
CREATE TABLE climb_history (
    
    -- mirror the "climbs" table.
    -- remove most of the constraints as they may change on the "climbs"
    -- table.
    revision int PRIMARY KEY,
    name varchar(60) NOT NULL,
    title varchar(100) NOT NULL,
    description text NOT NULL,
    crag_id int NOT NULL,
    grade_id int NOT NULL,

    -- onto the history data
    climb_id int,    -- note that this does *not* reference climbs.id
    timestamp timestamp without time zone NOT NULL DEFAULT localtimestamp
);

-- Setup a trigger that records inserts and updates into "climb_history".
CREATE FUNCTION record_climb() RETURNS TRIGGER AS $BODY$
BEGIN

    -- Record the new/updated climb.
    INSERT INTO climb_history (revision,
                               name,
                               title,
                               description,
                               crag_id,
                               grade_id,
                               climb_id)
        VALUES (NEW.revision,
                NEW.name,
                NEW.title,
                NEW.description,
                NEW.crag_id,
                NEW.grade_id,
                NEW.id);

    -- Bump the referenced crag's revision
    UPDATE crags SET
        revision = NEW.revision

    -- TODO: also bump the old crag in case of deletion.

    WHERE id = NEW.crag_id;

    RETURN NULL;
END;
$BODY$ LANGUAGE plpgsql;

CREATE TRIGGER on_climb_insert_or_update
    AFTER INSERT OR UPDATE ON climbs
    FOR EACH ROW EXECUTE PROCEDURE record_climb();
