# Part 2: How records survive closing the app

An ordinary Java variable disappears when the program exits. SQLite writes
records into a file, tracker.db, so another run can retrieve them.
SQLite runs inside the application; there is no database server to start.

## Startup sequence

1. TrackerApplication asks AppPaths for the database file location.
2. Database.open creates its parent directory and opens a JDBC connection.
3. Database.initialize reads SQLite's user_version.
4. A new database gets five tables in one transaction and schema version 1.
5. An existing version-1 database is reused. A newer schema is rejected.

A transaction makes schema creation all-or-nothing. Foreign keys are enabled on
every connection, so an update cannot refer to a project that does not exist.
Connections and statements close through Java try-with-resources.

## Tables and relationships

- projects: the student, project title, status and start date.
- updates: dated progress notes and next steps for a project.
- tasks: work with optional due and completion dates.
- followups: requests with optional due and resolution dates.
- import_runs: a record of completed imports.

Example: P001 identifies Student A's Graph Theory project. U001 references P001
and holds one weekly update. Many updates can reference the same project.
Project names can change without breaking that relationship.

## What this part does and does not implement

It establishes persistent storage and database constraints. It does not yet
offer forms or commands to create student records. Repository operations and
project/update models come in Part 3, when their concrete behaviour is needed.
Dates are stored as ISO text; the service layer will validate them as LocalDate.

## Verification

JUnit tests insert an anonymised project/update, close and reopen the database,
and confirm the update remains after repeated initialization. Separate tests
reject an unknown project reference and a database from a newer schema version.
Tests use temporary directories rather than the user's real database.

## Next part

Build Java project/update models and repository operations to add and retrieve
records without putting SQL in the user interface.
