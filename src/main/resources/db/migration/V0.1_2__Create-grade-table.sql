-- Grade schema

-- Each grading system is represented as an instance of the GradingSystem
-- enumeration.
--
-- The reason for having a UkTrad grading system is to allow multi-pitch uk
-- trad routes to give a pitch-by-pitch breakdown in either UkTechnical *or*
-- UkTrad.
CREATE TYPE GradingSystem as ENUM (
    'EuSport',              -- f7a, f7a+, ...
    'UkAdjective',          -- e1, e2, ...
    'UkTechnical',          -- 5a, 5b, 5c, ...
    'UkTrad',               -- e1 5a, e1 5b, e1 5c, ...
    'Font'                  -- F7a, F7a+, ...
);

CREATE TABLE grades (
    id serial PRIMARY KEY,
    grading_system GradingSystem NOT NULL,
    difficulty int NOT NULL,

    UNIQUE (grading_system, difficulty)
);

CREATE INDEX grades_difficulty ON grades ( grading_system, difficulty );

DO LANGUAGE plpgsql $$
BEGIN

    FOR i IN 1..33 LOOP
        INSERT INTO grades (grading_system, difficulty) VALUES ('EuSport', i);
    END LOOP;

    FOR i IN 1..18 LOOP
        INSERT INTO grades (grading_system, difficulty) VALUES ('UkTechnical', i);
    END LOOP;

    FOR i IN 1..27 LOOP
        INSERT INTO grades (grading_system, difficulty) VALUES ('UkAdjective', i);
    END LOOP;

    FOR i IN 1..486 LOOP
        INSERT INTO grades (grading_system, difficulty) VALUES ('UkTrad', i);
    END LOOP;
END
$$;

