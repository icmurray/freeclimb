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

-- A Grade is a composite type.  A Grade belongs to a particular grading
-- system, and grades within a grading system are ranked linearly accoding to
-- their difficulty.  There's no database code to map the difficulty ranking of
-- a grade to it's grade name.  This mapping is encoding in application code.
--
-- This is perhaps a non-standard approach, but it does allow indexing of the
-- climbs table on the grade column.  Compared to a separate "grades" table
-- reference from the climbs table, this should allow more efficient lookup of
-- climbs based on a grade range of a given GradingSystem.  Which will probably
-- be a fairly common request.
CREATE TYPE Grade AS (
    system GradingSystem,
    difficulty int
);

