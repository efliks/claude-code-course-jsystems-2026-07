# be-developer Memory Index

- [HSDC backend stack](project_hsdc_backend_stack.md) — course project uses Java 21 + Spring Boot + Maven under app/backend, overriding the Python-first AGENTS.md default.
- [Windows temp-file SQLite + WAL test cleanup](feedback_windows_sqlite_wal_tempdir.md) — always close HikariDataSource/WAL connections before @TempDir teardown on Windows or cleanup throws.
- [Commit scope discipline on shared repos](feedback_scoped_git_add_shared_repo.md) — use explicit `git add <path>`, never `-A`, when other agents/dirty files share the repo; verify with git status before committing.
- [SessionRecorder: TransactionTemplate over @Transactional for testability](feedback_programmatic_tx_for_testable_facades.md) — use programmatic transactions (TransactionTemplate/PlatformTransactionManager) instead of AOP `@Transactional` when a facade must be unit-testable by direct instantiation with mocks.
- [Verify fast-moving LLM SDK APIs via jar decompile](feedback_verify_llm_apis_via_jar_decompile.md) — when Context7 fails, `javap` the actual resolved jar instead of trusting only WebFetch/README summaries.
- [WireMock 3.13.x needs wiremock-jetty12](feedback_wiremock_3_13_needs_jetty12_module.md) — add wiremock-jetty12 + exclude wiremock core's transitive Jetty 11 deps, or server startup throws.
- [DEBUG log-capture tests must silence the test HTTP server's own loggers](feedback_debug_log_capture_test_scope_test_server.md) — Jetty/WireMock DEBUG-log the raw Authorization header as normal request-echo; not an app leak.
