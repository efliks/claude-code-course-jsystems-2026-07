---
name: feedback-windows-sqlite-wal-tempdir
description: On Windows, JUnit @TempDir cleanup fails after WAL-mode SQLite tests unless the DataSource/HikariDataSource is explicitly closed first.
metadata:
  type: feedback
---

When a JUnit test opens a SQLite database in WAL journal mode inside a
`@TempDir`-managed directory (via a Hikari pool, as built by
`DataSourceConfig.buildDataSource`), the driver keeps `-wal`/`-shm` sidecar
files open. On Windows, an open file handle blocks deletion outright (no
POSIX unlink-while-open semantics), so JUnit's post-test `@TempDir` cleanup
throws `FileSystemException: ... being used by another process` even though
every individual test assertion passed.

**Why:** discovered while building the ADR-004 SQLite persistence layer
(HSDC course project) — `DataSourceConfigTest` passed all assertions but
the *test run itself* failed because temp-dir teardown couldn't delete the
still-open `.db-wal`/`.db-shm` files.

**How to apply:** any test helper that opens a real SQLite `DataSource`
against a `@TempDir` path must return a `Closeable`/`HikariDataSource` (not
a bare `javax.sql.DataSource`) so tests can `try (HikariDataSource ds = ...)`
or explicit `@AfterEach ds.close()` before the temp dir is torn down. Applied
in `TestDatabases.fresh()/open()` in the HSDC backend
(`app/backend/src/test/java/.../persistence/support/TestDatabases.java`).
This is Windows-specific; the same code would clean up fine on Linux/macOS
even without closing, but always closing is the portable-correct habit.
